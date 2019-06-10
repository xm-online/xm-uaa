package com.icthh.xm.uaa.service.tenant;

import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;
import static com.icthh.xm.uaa.config.Constants.CHANGE_LOG_PATH;
import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.uaa.util.DatabaseUtil;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@IgnoreLogginAspect
public class TenantDatabaseService {

    private DataSource dataSource;
    private LiquibaseProperties liquibaseProperties;
    private ResourceLoader resourceLoader;
    private DropSchemaResolver schemaDropResolver;

    /**
     * Create database schema for tenant.
     *
     * @param tenantKey - the tenant key
     */
    public void create(String tenantKey) {
        final StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant:schema tenantKey: {}", tenantKey);
        try {
            DatabaseUtil.createSchema(dataSource, tenantKey);
            log.info("STOP  - SETUP:CreateTenant:schema tenantKey: {}, result: OK, time = {} ms", tenantKey,
                stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:CreateTenant:schema tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenantKey, e.getMessage(), stopWatch.getTime());
            throw e;
        }

    }

    /**
     * Migrate database with liquibase.
     *
     * @param tenantKey the tenant key
     */
    @SneakyThrows
    public void migrate(String tenantKey) {
        final StopWatch stopWatch = createStarted();
        try {
            log.info("START - SETUP:CreateTenant:liquibase tenantKey: {}", tenantKey);
            assertTenantKeyValid(tenantKey);
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(CHANGE_LOG_PATH);
            liquibase.setContexts(liquibaseProperties.getContexts());
            liquibase.setDefaultSchema(tenantKey.toLowerCase());
            liquibase.setDropFirst(liquibaseProperties.isDropFirst());
            liquibase.setChangeLogParameters(DatabaseUtil.defaultParams(tenantKey));
            liquibase.setShouldRun(true);
            liquibase.afterPropertiesSet();
            log.info("STOP  - SETUP:CreateTenant:liquibase tenantKey: {}, result: OK, time = {} ms", tenantKey,
                stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:CreateTenant:liquibase tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenantKey, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

    /**
     * Drop database schema for tenant.
     *
     * @param tenantKey - the tenant key
     */
    @SneakyThrows
    public void drop(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant:schema tenantKey: {}", tenantKey);
        assertTenantKeyValid(tenantKey);
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(schemaDropResolver.getSchemaDropCommand(), tenantKey));
            log.info("STOP  - SETUP:DeleteTenant:schema tenantKey: {}, result: OK, time = {} ms",
                tenantKey, stopWatch.getTime());
        } catch (Exception e) {
            log.info("STOP  - SETUP:DeleteTenant:schema tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenantKey, e.getMessage(), stopWatch.getTime());
            throw e;
        }
    }

}
