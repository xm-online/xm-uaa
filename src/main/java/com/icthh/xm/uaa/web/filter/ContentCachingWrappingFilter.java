package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.uaa.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
@Order(1)
public class ContentCachingWrappingFilter extends OncePerRequestFilter {

    private final ApplicationProperties applicationProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Integer cacheLimit = applicationProperties.getRequestCacheLimit();
        ContentCachingRequestWrapper requestWrapper = cacheLimit != null ?
            new ContentCachingRequestWrapper(request, cacheLimit) :
            new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }
}
