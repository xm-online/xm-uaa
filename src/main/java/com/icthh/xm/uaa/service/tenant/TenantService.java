package com.icthh.xm.uaa.service.tenant;

import static com.icthh.xm.uaa.config.Constants.DEFAULT_EMAILS_PATH_PATTERN;
import static com.icthh.xm.uaa.config.Constants.DEFAULT_EMAILS_PATTERN;
import static com.icthh.xm.uaa.config.Constants.PATH_TO_EMAILS;
import static com.icthh.xm.uaa.config.Constants.PATH_TO_EMAILS_IN_CONFIG;
import static com.icthh.xm.uaa.util.GeneralUtils.sneakyThrows;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.Constants;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.icthh.xm.uaa.config.tenant.SchemaChangeResolver;
import com.icthh.xm.uaa.service.ClientService;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.internal.SessionImpl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import javax.persistence.EntityManager;

@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class TenantService {

    private static final String API = "/api";
    public static final String CLIENT_ID = "webapp";
    public static final String CLIENT_SECRET = "webapp";
    public static final String ROLE_KEY = "ROLE_ANONYMOUS";

    private final TenantDatabaseService databaseService;
    private final TenantListRepository tenantListRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final ApplicationProperties applicationProperties;
    private final PermissionProperties permissionProperties;
    private final ResourceLoader resourceLoader;
    private final ClientService clientRepository;
    private final TenantContextHolder tenantContextHolder;
    private final EntityManager em;
    private final SchemaChangeResolver schemaChangeResolver;
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Create tenant.
     * @param tenant tenant key
     */
    public void createTenant(String tenant) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:CreateTenant: tenantKey: {}", tenant);

        try {
            tenantListRepository.addTenant(tenant);
            databaseService.create(tenant);
            databaseService.migrate(tenant);
            addDefaultOauth2Client(tenant);
            addUaaSpecification(tenant);

            addLoginsSpecification(tenant);
            addRoleSpecification(tenant);
            addPermissionSpecification(tenant);
            addDefaultEmailTemplates(tenant);

            log.info("STOP  - SETUP:CreateTenant: tenantKey: {}, result: OK, time = {} ms",
                tenant, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:CreateTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenant, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    @SneakyThrows
    private void addDefaultOauth2Client(String tenant) {
        SessionImpl delegate = (SessionImpl) em.getDelegate();
        Connection connection = delegate.connection();
        Statement statement = connection.createStatement();
        statement.execute(String.format(schemaChangeResolver.getSchemaSwitchCommand(), tenant));
        ClientDTO u = new ClientDTO();
        u.setClientId(CLIENT_ID);
        u.setClientSecret(CLIENT_SECRET);
        u.setRoleKey(ROLE_KEY);
        clientRepository.createClient(u);

    }

    /**
     * Delete tenant.
     * @param tenant tenant key
     */
    public void deleteTenant(String tenant) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:DeleteTenant: tenantKey: {}", tenant);

        try {
            databaseService.drop(tenant);
            tenantListRepository.deleteTenant(tenant);

            tenantConfigRepository.deleteConfig(tenant.toUpperCase(),
                "/" + applicationProperties.getTenantPropertiesName());
            tenantConfigRepository.deleteConfig(tenant.toUpperCase(),
                "/" + applicationProperties.getTenantLoginPropertiesName());
            tenantConfigRepository.deleteConfigFullPath(tenant.toUpperCase(),
                API + permissionProperties.getRolesSpecPath());
            tenantConfigRepository.deleteConfigFullPath(tenant.toUpperCase(),
                API + permissionProperties.getPermissionsSpecPath());

            log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: OK, time = {} ms",
                tenant, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenant, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    public void manageTenant(String tenant, String state) {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("START - SETUP:ManageTenant: tenantKey: {}, state: {}", tenant, state);

        try {
            tenantListRepository.updateTenant(tenant.toLowerCase(), state.toUpperCase());

            log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: OK, time = {} ms",
                tenant, state, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: FAIL, error: {}, time = {} ms",
                tenant, state, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    @SneakyThrows
    private void addUaaSpecification(String tenantName) {
        String specificationName = applicationProperties.getTenantPropertiesName();
        InputStream in = new ClassPathResource(Constants.DEFAULT_CONFIG_PATH).getInputStream();
        String specification = IOUtils.toString(in, UTF_8);
        tenantConfigRepository.updateConfig(tenantName, "/" + specificationName, specification);
    }

    @SneakyThrows
    private void addDefaultEmailTemplates(String tenantName) {
        Resource[] resources = getResourcePatternResolver(resourceLoader).getResources(DEFAULT_EMAILS_PATTERN);
        stream(resources).forEach(sneakyThrows(resource -> {
            AntPathMatcher matcher = new AntPathMatcher();
            String path = resource.getURL().getPath();
            int startIndex = path.indexOf(PATH_TO_EMAILS);
            if (startIndex == -1) {
                return;
            }
            path = path.substring(startIndex);

            Map<String, String> fileParams = matcher.extractUriTemplateVariables(DEFAULT_EMAILS_PATH_PATTERN, path);
            String email = IOUtils.toString(resource.getInputStream(), UTF_8);
            String configPath = PATH_TO_EMAILS_IN_CONFIG + fileParams.get("lang") + "/" + fileParams.get("name") + ".ftl";
            tenantConfigRepository.updateConfig(tenantName, configPath, email);
        }));
    }

    @SneakyThrows
    private void addRoleSpecification(String tenantName) {
        String rolesYml = mapper.writeValueAsString(new HashMap<>());

        tenantConfigRepository.updateConfigFullPath(tenantName,
            API + permissionProperties.getRolesSpecPath(), rolesYml);
    }

    @SneakyThrows
    private void addPermissionSpecification(String tenantName) {

        String permissionsYml = mapper.writeValueAsString(new HashMap<>());

        tenantConfigRepository.updateConfigFullPath(tenantName, API + permissionProperties.getPermissionsSpecPath(),
            permissionsYml);
    }

    @SneakyThrows
    private void addLoginsSpecification(String tenantName) {
        String specificationName = applicationProperties.getTenantLoginPropertiesName();
        InputStream in = new ClassPathResource(Constants.DEFAULT_LOGINS_CONFIG_PATH).getInputStream();
        String specification = IOUtils.toString(in, UTF_8);
        tenantConfigRepository.updateConfig(tenantName, "/" + specificationName, specification);
    }
}
