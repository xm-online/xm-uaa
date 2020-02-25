package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.SneakyThrows;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@LepService(group = "service.auth.filter")
public class CustomUserAuthenticationService {

    @SneakyThrows
    @LogicExtensionPoint("DoFilter")
    void process(ServletRequest request, ServletResponse response, FilterChain chain) {
        chain.doFilter(request, response);
    }
}
