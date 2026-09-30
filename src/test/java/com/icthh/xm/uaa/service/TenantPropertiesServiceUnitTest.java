package com.icthh.xm.uaa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.security.DefaultClientSecretPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class TenantPropertiesServiceUnitTest {

    private static final String CONFIG_KEY = "/config/tenants/TEST/uaa/uaa.yml";

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private TenantConfigRepository tenantConfigRepository;
    @Mock
    private TenantContextHolder tenantContextHolder;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger policyLogger = (Logger) LoggerFactory.getLogger(DefaultClientSecretPolicy.class);

    private TenantPropertiesService service;

    @Before
    public void setUp() {
        when(applicationProperties.getTenantPropertiesPathPattern())
            .thenReturn("/config/tenants/{tenantName}/uaa/uaa.yml");
        service = new TenantPropertiesService(applicationProperties, tenantConfigRepository, tenantContextHolder);
        appender.start();
        policyLogger.addAppender(appender);
    }

    @After
    public void tearDown() {
        policyLogger.detachAppender(appender);
    }

    @Test
    public void shouldLogErrorWhenDefaultClientSecretIsMissing() {
        service.onRefresh(CONFIG_KEY, "security:\n    defaultUserRole: ROLE_USER\n");

        assertThat(errors()).containsExactly("not set");
        assertThat(service.getTenantProps(TenantKey.valueOf("TEST"))).isNotNull();
    }

    @Test
    public void shouldLogErrorWhenDefaultClientSecretIsWellKnown() {
        service.onRefresh(CONFIG_KEY, "security:\n    defaultClientSecret: internal\n");

        assertThat(errors()).containsExactly("well-known");
    }

    @Test
    public void shouldNotLogErrorWhenDefaultClientSecretIsStrong() {
        service.onRefresh(CONFIG_KEY, "security:\n    defaultClientSecret: 7c1e0b2a-strong\n");

        assertThat(errors()).isEmpty();
    }

    private List<String> errors() {
        return appender.list.stream()
            .filter(e -> e.getLevel() == Level.ERROR)
            .map(e -> e.getFormattedMessage().contains("well-known") ? "well-known" : "not set")
            .collect(Collectors.toList());
    }
}
