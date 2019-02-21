package com.icthh.xm.uaa.config;

import com.codahale.metrics.MetricRegistry;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import com.zaxxer.hikari.HikariDataSource;

import io.github.jhipster.config.JHipsterProperties;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableMetrics(proxyTargetClass = true)
public class AppMetricsConfiguration extends MetricsConfigurerAdapter implements ServletContextInitializer {

    private final MetricRegistry metricRegistry;

    private final JHipsterProperties jhipsterProperties;

    private HikariDataSource hikariDataSource;

    public AppMetricsConfiguration(MetricRegistry metricRegistry, JHipsterProperties jhipsterProperties) {
        this.metricRegistry = metricRegistry;
        this.jhipsterProperties = jhipsterProperties;
    }

    @Autowired(required = false)
    public void setHikariDataSource(HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
    }

    @PostConstruct
    public void init() {
        if (hikariDataSource != null) {
            log.debug("Monitoring the datasource");
            // remove the factory created by HikariDataSourceMetricsPostProcessor until JHipster migrate to Micrometer
            hikariDataSource.setMetricsTrackerFactory(null);
            hikariDataSource.setMetricRegistry(metricRegistry);
        }
    }

    @Override
    public void onStartup(ServletContext servletContext) {

        if (jhipsterProperties.getMetrics().getPrometheus().isEnabled()) {
            String endpoint = jhipsterProperties.getMetrics().getPrometheus().getEndpoint();

            log.debug("Initializing prometheus metrics exporting via {}", endpoint);

            CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));
            servletContext
                .addServlet("prometheusMetrics", new MetricsServlet(CollectorRegistry.defaultRegistry))
                .addMapping(endpoint);
        }
    }
}
