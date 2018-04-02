package com.icthh.xm.uaa.config;

import static org.mockito.Mockito.mock;

import com.icthh.xm.uaa.social.connect.web.ConnectSupport;
import com.icthh.xm.uaa.social.connect.web.SessionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SocialBeanOverrideConfiguration {

    @Bean
    @Primary
    public SessionStrategy sessionStrategy() {
        return mock(SessionStrategy.class);
    }

    @Bean
    @Primary
    public ConnectSupport connectSupport() {
        return mock(ConnectSupport.class);
    }

}
