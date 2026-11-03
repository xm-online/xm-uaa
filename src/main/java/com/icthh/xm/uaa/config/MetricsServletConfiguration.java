package com.icthh.xm.uaa.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@ManagementContextConfiguration
public class MetricsServletConfiguration {

    @Bean
    public ServletRegistrationBean<MetricsServlet> metricsServletRegistration(MetricRegistry metricRegistry) {
        ServletRegistrationBean<MetricsServlet> registration =
            new ServletRegistrationBean<>(new MetricsServlet(metricRegistry), "/management/metrics/*");
        registration.setName("metricsServlet");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(2);
        return registration;
    }
}
