package com.icthh.xm.uaa.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test JPA configuration to enable scanning of test entities and repositories.
 */
@Configuration
@EntityScan(basePackages = "com.icthh.xm.uaa.domain")
@EnableJpaRepositories(basePackages = "com.icthh.xm.uaa.repository")
public class TestJpaConfiguration {
}