package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.uaa.service.RequestEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Custom filter to add logic extension point to filter chain
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestEnrichmentFilter implements Filter {

    private final RequestEnrichmentService requestEnrichmentService;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        log.trace("execute request enrichment filter");
        ServletRequest enrichedRequest = requestEnrichmentService.enrichRequest(request);
        chain.doFilter(enrichedRequest, response);
        log.trace("end execution of request enrichment filter");
    }
}
