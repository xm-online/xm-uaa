package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter for logging all HTTP requests.
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class LoggingFilter implements Filter {
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
    throws IOException, ServletException {

        StopWatch stopWatch = StopWatch.createStarted();

        String remoteAddr = request.getRemoteAddr();
        Long contentLength = request.getContentLengthLong();

        String method = null;
        String requestUri = null;

        try {

            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = HttpServletRequest.class.cast(request);
                method = req.getMethod();
                requestUri = req.getRequestURI();
            }

            log.info("START {} --> {} {}, contentLength = {} ", remoteAddr, method, requestUri,
                     contentLength);

            chain.doFilter(request, response);

            Integer status = null;
            boolean httpSuccess = true;

            if (response instanceof HttpServletResponse) {
                HttpServletResponse res = (HttpServletResponse) response;
                status = res.getStatus();
                httpSuccess = status >= 200 && status < 300;
            }

            if (httpSuccess) {
                log.info("STOP  {} --> {} {}, status = {}, time = {} ms",
                         remoteAddr, method, requestUri, status, stopWatch.getTime());
            } else {
                log.warn("STOP  {} --> {} {}, status = {}, time = {} ms",
                         remoteAddr, method, requestUri, status, stopWatch.getTime());
            }

        } catch (Exception e) {
            log.error("STOP  {} --> {} {}, error = {}, time = {} ms",
                      remoteAddr, method, requestUri, LogObjectPrinter.printException(e), stopWatch.getTime());
            throw e;
        }

    }

    @Override
    public void destroy() {

    }
}
