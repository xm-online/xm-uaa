package com.icthh.xm.uaa.web.filter;

import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_KEY;
import static com.icthh.xm.uaa.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.HEADER_PORT;
import static com.icthh.xm.uaa.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static com.icthh.xm.uaa.config.Constants.HEADER_WEBAPP_URL;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import com.icthh.xm.uaa.security.SecurityUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Extract TenantInfo to TenantContext.
 */
@RequiredArgsConstructor
@Component
public class ProxyFilter implements Filter {

    private final ApplicationProperties applicationProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TenantListRepository tenantListRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // skip ignored requests
        if (isIgnoredRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String protocol = httpRequest.getHeader(HEADER_SCHEME);
        String domain = httpRequest.getHeader(HEADER_DOMAIN);
        String port = httpRequest.getHeader(HEADER_PORT);
        String tenant = getTenant(httpRequest);
        String webapp = httpRequest.getHeader(HEADER_WEBAPP_URL);
        String userLogin = SecurityUtils.getCurrentUserLogin();

        if (StringUtils.isBlank(tenant)) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write("{\"error\": \"No tenant supplied\"}");
            httpResponse.getWriter().flush();
            return;
        } else if (tenantListRepository.getSuspendedTenants().contains(tenant.toLowerCase())) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write("{\"error\": \"Tenant is suspended\"}");
            httpResponse.getWriter().flush();
            return;
        }

        TenantContext.setCurrent(new TenantInfo(tenant.toUpperCase(), userLogin, getUserKey(), protocol, domain, port, webapp));

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public void destroy() {
        // no logic here
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // no logic here
    }

    @SuppressWarnings("unchecked")
    private static String getTenant(HttpServletRequest request) {
        final OAuth2Authentication auth = getAuthentication();
        if (auth == null) {
            return request.getHeader(HEADER_TENANT);
        } else if (auth.getDetails() != null) {
            Map<String, String> details = firstNonNull((Map)(((OAuth2AuthenticationDetails) auth.getDetails())
                .getDecodedDetails()), new HashMap<>());
            return details.getOrDefault(AUTH_TENANT_KEY, "");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String getUserKey() {
        final OAuth2Authentication auth = getAuthentication();
        if (auth != null && auth.getDetails() != null) {
            Map<String, String> details = firstNonNull((Map)(((OAuth2AuthenticationDetails) auth.getDetails())
                .getDecodedDetails()), new HashMap<>());
            return details.getOrDefault(AUTH_USER_KEY, "");
        }
        return null;
    }

    private boolean isIgnoredRequest(HttpServletRequest request) {
        String path = request.getServletPath();
        List<String> ignoredPatterns = applicationProperties.getTenantIgnoredPathList();
        if (ignoredPatterns != null && path != null) {
            for (String pattern : ignoredPatterns) {
                if (matcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static OAuth2Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OAuth2Authentication) {
            return (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        }
        return null;
    }
}
