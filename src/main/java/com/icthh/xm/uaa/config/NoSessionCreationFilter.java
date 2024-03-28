package com.icthh.xm.uaa.config;

import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class NoSessionCreationFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public HttpSession getSession() {
                return this.getSession(false);
            }

            @Override
            public HttpSession getSession(boolean create) {
                return super.getSession(false);
            }
        };
        chain.doFilter(wrappedRequest, response);
    }
}
