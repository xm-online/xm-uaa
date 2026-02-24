package com.icthh.xm.uaa;

import com.icthh.xm.commons.lep.spring.LepSpringConfiguration;
import com.icthh.xm.uaa.config.AsyncSyncConfiguration;
import com.icthh.xm.uaa.config.JacksonHibernateConfiguration;
import com.icthh.xm.uaa.config.TestJpaConfiguration;
import com.icthh.xm.uaa.config.TestTenantConfigConfiguration;
import com.icthh.xm.uaa.config.testcontainer.DatabaseTestContainer;
import com.icthh.xm.uaa.config.JacksonConfiguration;
import com.icthh.xm.uaa.config.testcontainer.KafkaTestContainer;
import com.icthh.xm.uaa.config.TestSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        UaaApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        TestJpaConfiguration.class,
        TestTenantConfigConfiguration.class,
    }
)
@ImportTestcontainers({ DatabaseTestContainer.class, KafkaTestContainer.class })
public @interface IntegrationTest {}
