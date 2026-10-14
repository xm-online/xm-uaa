package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PORT;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PROTOCOL;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_WEB_APP;

/**
 * Web application url and domain of the current request, used in links sent to users
 * (activation, password reset, otp). Values come from x-webapp-url, x-scheme, x-domain, x-port request headers,
 * so they are client controlled: host must belong to the tenant ({@link TenantDomainsService} or 'securityPolicy.hosts' in uaa.yml).
 * Check is skipped when no hosts are configured for the tenant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationUrlService {

    public static final String ERROR_HOST_NOT_ALLOWED = "error.application.host.not.allowed";

    private final XmRequestContextHolder xmRequestContextHolder;
    private final TenantContextHolder tenantContextHolder;
    private final TenantDomainsService tenantDomainsService;
    private final TenantPropertiesService tenantPropertiesService;

    /**
     * Get web application url.
     *
     * @return x-webapp-url, or url built from x-scheme, x-domain and x-port
     * @throws BusinessException when url host does not belong to the tenant
     */
    public String getApplicationUrl() {
        XmRequestContext context = xmRequestContextHolder.getContext();
        String webApp = getValue(context, REQUEST_CTX_WEB_APP);
        String url = StringUtils.isNotBlank(webApp) ? webApp : buildUrl(context);
        assertHostAllowed(getHttpHost(url));
        return url;
    }

    /**
     * Get request domain.
     *
     * @return x-domain
     * @throws BusinessException when domain does not belong to the tenant
     */
    public String getDomain() {
        String domain = getValue(xmRequestContextHolder.getContext(), REQUEST_CTX_DOMAIN);
        assertHostAllowed(domain);
        return domain;
    }

    private void assertHostAllowed(String host) {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
        Set<String> domains = tenantDomainsService.getDomains(tenantKey);
        List<String> hosts = tenantPropertiesService.getTenantProps().getSecurityPolicy().getHosts();
        if (domains.isEmpty() && hosts.isEmpty()) {
            return;
        }
        String lowerHost = StringUtils.lowerCase(host);
        if (lowerHost == null || !(domains.contains(lowerHost) || hosts.contains(lowerHost))) {
            log.warn("Host {} does not belong to the tenant, allowed: {} {}", host, domains, hosts);
            throw new BusinessException(ERROR_HOST_NOT_ALLOWED, "Host is not allowed: " + host);
        }
    }

    private static String buildUrl(XmRequestContext context) {
        String port = getValue(context, REQUEST_CTX_PORT);
        String urlPort = StringUtils.isBlank(port) || "80".equals(port) ? "" : (":" + port);
        return getValue(context, REQUEST_CTX_PROTOCOL) + "://" + getValue(context, REQUEST_CTX_DOMAIN) + urlPort;
    }

    private static String getHttpHost(String url) {
        try {
            URI uri = new URI(url);
            boolean isHttp = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            return isHttp ? uri.getHost() : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String getValue(XmRequestContext context, String key) {
        return context.getValue(key, String.class);
    }
}
