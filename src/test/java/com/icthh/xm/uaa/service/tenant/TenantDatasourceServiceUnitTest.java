package com.icthh.xm.uaa.service.tenant;

import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.tenant.SchemaDropResolver;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.icthh.xm.uaa.config.Constants.DDL_CREATE_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantDatasourceServiceUnitTest {

    private static final String DROP_COMMAND = "DROP COMMAND";

    @InjectMocks
    private TenantDatabaseService service;
    @Mock
    private DataSource dataSource;
    @Mock
    private LiquibaseProperties liquibaseProperties;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private SchemaDropResolver schemaDropResolver;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    public void testCreateSuccess() throws Exception {
        service.create(Constants.DEFAULT_TENANT);

        verify(statement).executeUpdate(String.format(DDL_CREATE_SCHEMA, Constants.DEFAULT_TENANT));
    }

    @Test
    public void testExecuteException() throws Exception {
        when(statement.executeUpdate(any())).thenThrow(new SQLException());

        try {
            service.create(Constants.DEFAULT_TENANT);
        } catch (Exception e) {
            assertEquals("Can not connect to database", e.getMessage());
        }

        verify(statement).executeUpdate(String.format(DDL_CREATE_SCHEMA, Constants.DEFAULT_TENANT));
    }

    @Test
    public void testMigrateSuccess() throws Exception {
        LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class)
            .setValue(GlobalConfiguration.SHOULD_RUN, false);

        service.migrate(Constants.DEFAULT_TENANT);
    }

    @Test
    public void testMigrateException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("exception"));

        try {
            service.migrate(Constants.DEFAULT_TENANT);
        } catch (Exception e) {
            assertEquals("Can not migrate database for creation tenant "
                + Constants.DEFAULT_TENANT, e.getMessage());
        }
    }

    @Test
    public void testDropSuccess() throws Exception {
        when(schemaDropResolver.getSchemaDropCommand()).thenReturn(DROP_COMMAND);

        service.drop(Constants.DEFAULT_TENANT);

        verify(statement).executeUpdate(DROP_COMMAND);
    }

    @Test
    public void testDropException() throws Exception {
        when(schemaDropResolver.getSchemaDropCommand()).thenReturn(DROP_COMMAND);
        when(statement.executeUpdate(any())).thenThrow(new SQLException("drop exception"));

        try {
            service.drop(Constants.DEFAULT_TENANT);
        } catch (Exception e) {
            assertEquals("drop exception", e.getMessage());
        }

        verify(statement).executeUpdate(DROP_COMMAND);
    }
}
