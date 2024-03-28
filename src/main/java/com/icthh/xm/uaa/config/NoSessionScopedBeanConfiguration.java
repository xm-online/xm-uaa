package com.icthh.xm.uaa.config;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestScope;

@Configuration
public class NoSessionScopedBeanConfiguration {

    public NoSessionScopedBeanConfiguration(ConfigurableListableBeanFactory beanFactory){
        beanFactory.registerScope("session", new NoSessionScope());
    }

    public static class NoSessionScope extends RequestScope {

    }
}
