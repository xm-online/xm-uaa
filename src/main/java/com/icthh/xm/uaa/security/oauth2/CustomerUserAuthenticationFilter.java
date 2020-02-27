package com.icthh.xm.uaa.security.oauth2;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import static com.icthh.xm.uaa.config.UaaFilterOrders.CUSTOM_AUTHENTICATION_FILTER_ORDER;

@Slf4j
@Component
@Order(CUSTOM_AUTHENTICATION_FILTER_ORDER)
public class CustomerUserAuthenticationFilter implements Filter {

    @Autowired
    private CustomAuthenticationFilterProcessor customAuthenticationFilterProcessor;

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        log.info("Run custom authentication filter processing ");

        if (((HttpServletRequest) request).getRequestURI().contains("/oauth/token"))
            customAuthenticationFilterProcessor.process(request, response, chain);
        else
            chain.doFilter(request, response);
    }
}
