package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.commons.base.DefaultXmRequestContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link XmRequestContextConfiguration} class.
 */
@Configuration
public class XmRequestContextConfiguration {

    @Bean
    XmRequestContextHolder XmRequestContextHolder() {
        return new DefaultXmRequestContextHolder();
    }

}
