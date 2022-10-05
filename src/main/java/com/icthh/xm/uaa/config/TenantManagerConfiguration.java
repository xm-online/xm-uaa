package com.icthh.xm.uaa.config;

import static com.icthh.xm.commons.config.domain.Configuration.of;
import static com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner.prependTenantPath;
import static com.icthh.xm.uaa.config.Constants.DEFAULT_CONFIG_PATH;
import static com.icthh.xm.uaa.config.Constants.DEFAULT_EMAILS_PATH_PATTERN;
import static com.icthh.xm.uaa.config.Constants.DEFAULT_EMAILS_PATTERN;
import static com.icthh.xm.uaa.config.Constants.DEFAULT_LOGINS_CONFIG_PATH;
import static com.icthh.xm.uaa.config.Constants.PATH_TO_EMAILS;
import static com.icthh.xm.uaa.config.Constants.PATH_TO_EMAILS_IN_CONFIG;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.migration.db.tenant.provisioner.TenantDatabaseProvisioner;
import com.icthh.xm.commons.permission.config.PermissionProperties;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantConfigProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.context.annotation.Configuration
public class TenantManagerConfiguration {

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private AntPathMatcher matcher = new AntPathMatcher();
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public TenantManager tenantManager(
        TenantAbilityCheckerProvisioner abilityCheckerProvisioner,
        TenantDatabaseProvisioner databaseProvisioner,
        TenantConfigProvisioner configProvisioner,
        TenantListProvisioner tenantListProvisioner) {

        TenantManager manager = TenantManager.builder()
                                             .service(abilityCheckerProvisioner)
                                             .service(tenantListProvisioner)
                                             .service(databaseProvisioner)
                                             .service(configProvisioner)
                                             .build();
        log.info("Configured tenant manager: {}", manager);
        return manager;
    }

    @SneakyThrows
    @Bean
    public TenantConfigProvisioner tenantConfigProvisioner(TenantConfigRepository tenantConfigRepository,
                                                           PermissionProperties permissionProperties,
                                                           ApplicationProperties applicationProperties,
                                                           ResourceLoader resourceLoader) {

        Resource[] resources = getResourcePatternResolver(resourceLoader).getResources(DEFAULT_EMAILS_PATTERN);

        List<Configuration> emailConfigs = stream(resources).filter(pathIsExpected())
                                                            .map(this::resourceToConfiguration)
                                                            .collect(Collectors.toList());

        TenantConfigProvisioner provisioner = TenantConfigProvisioner
            .builder()
            .tenantConfigRepository(tenantConfigRepository)
            .configuration(of().path(permissionProperties.getRolesSpecPath())
                               .content(getEmptyYml())
                               .build())
            .configuration(of().path(permissionProperties.getPermissionsSpecPath())
                               .content(getEmptyYml())
                               .build())
            .configuration(of().path(applicationProperties.getTenantPropertiesPathPattern())
                               .content(readResource(DEFAULT_CONFIG_PATH))
                               .build())
            .configuration(of().path(applicationProperties.getTenantLoginPropertiesPathPattern())
                               .content(readResource(DEFAULT_LOGINS_CONFIG_PATH))
                               .build())
            .configurations(emailConfigs)
            .build();

        log.info("Configured tenant config provisioner: {}", provisioner);
        return provisioner;
    }

    String getApplicationName() {
        return applicationName;
    }

    private Predicate<Resource> pathIsExpected() {
        return (Resource resource) -> {
            try {
                return Optional.ofNullable(resource.getURL().getPath())
                               .map(f -> f.contains(PATH_TO_EMAILS))
                               .orElse(false);
            } catch (IOException e) {
                log.warn("can not get url for resource: {}", resource);
                return false;
            }
        };
    }

    private String getEmptyYml() throws JsonProcessingException {
        return mapper.writeValueAsString(new HashMap<>());
    }

    @SneakyThrows
    private String readResource(String location) {
        return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
    }

    @SneakyThrows
    private Configuration resourceToConfiguration(final Resource resource) {
        String configPath = toFullPath(extractEmailConfigPath(resource));
        String content = IOUtils.toString(resource.getInputStream(), UTF_8);
        return of().path(configPath).content(content).build();
    }

    @SneakyThrows
    private String extractEmailConfigPath(final Resource resource) {
        String path = resource.getURL().getPath();
        int startIndex = path.indexOf(PATH_TO_EMAILS);
        path = path.substring(startIndex);

        Map<String, String> fileParams = matcher.extractUriTemplateVariables(DEFAULT_EMAILS_PATH_PATTERN, path);
        return PATH_TO_EMAILS_IN_CONFIG + fileParams.get("lang") + "/" + fileParams.get("name") + ".ftl";
    }

    private String toFullPath(String path) {
        return prependTenantPath(Paths.get(getApplicationName(), path).toString());
    }

}
