package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Slf4j
@Component
@LepService(group = "service.auth.filter")
public class CustomerUserAuthenticationFilter extends GenericFilterBean {

    @Override
    @SneakyThrows
    @LogicExtensionPoint("DoFilter")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        chain.doFilter(request, response);
    }

}
