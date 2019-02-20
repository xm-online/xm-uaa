package com.icthh.xm.uaa.config;

import freemarker.template.TemplateException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

@Configuration
public class FreeMarkerConfiguration {

    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration() {
        UaaFreeMarkerConfigurationFactoryBean bean = new UaaFreeMarkerConfigurationFactoryBean();
        bean.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return bean.getObject();
    }

    /**
     * Overrides FreeMarkerConfigurationFactoryBean to use Freemarker template version 2.3.23
     * <p>
     * Last stable version can be seen here: https://mvnrepository.com/artifact/org.freemarker/freemarker
     * </p>
     */
    private static class UaaFreeMarkerConfigurationFactoryBean extends FreeMarkerConfigurationFactoryBean {

        @Override
        protected freemarker.template.Configuration newConfiguration() throws IOException, TemplateException {
            return new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_23);
        }

    }
}
