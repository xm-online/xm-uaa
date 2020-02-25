package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Slf4j
@Component
@LepService(group = "service.auth.filter")
public class CustomerUserAuthenticationFilter implements Filter {

    @Override
    @SneakyThrows
    @LogicExtensionPoint("DoFilter")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        log.info("Run custom authentication user filter");
        chain.doFilter(request, response);
    }

}
