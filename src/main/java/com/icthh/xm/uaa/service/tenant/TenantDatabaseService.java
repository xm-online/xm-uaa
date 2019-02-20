package com.icthh.xm.uaa.service.tenant;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.uaa.config.tenant.SchemaDropResolver;
import com.icthh.xm.uaa.util.DatabaseUtil;
import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static com.icthh.xm.uaa.config.Constants.CHANGE_LOG_PATH;
import static org.apache.commons.lang3.time.StopWatch.createStarted;

@Service
@AllArgsConstructor
@Slf4j
@IgnoreLogginAspect
public class TenantDatabaseService {

    private DataSource dataSource;
    private LiquibaseProperties liquibaseProperties;
    private ResourceLoader resourceLoader;
    private SchemaDropResolver schemaDropResolver;

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
     * @param tenantKey the tenant key
     */
    @SneakyThrows
    public void migrate(String tenantKey) {
        final StopWatch stopWatch = createStarted();
        try {
            log.info("START - SETUP:CreateTenant:liquibase tenantKey: {}", tenantKey);
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(CHANGE_LOG_PATH);
            liquibase.setContexts(liquibaseProperties.getContexts());
            liquibase.setDefaultSchema(tenantKey);
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
