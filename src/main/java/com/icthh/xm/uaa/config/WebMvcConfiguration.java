package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.lep.spring.web.LepInterceptor;
import com.icthh.xm.commons.web.spring.config.XmMsWebConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Used to register LEP interceptor (to be able to retrieve values from authContext.getDetialValue()).
 */
@RequiredArgsConstructor
@Configuration
@Import({XmMsWebConfiguration.class})
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    private final LepInterceptor lepInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(lepInterceptor).addPathPatterns("/**");
    }

}
