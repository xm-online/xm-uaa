package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.commons.XmPrivilegedRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.service.LepRequestEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME;
import static com.icthh.xm.uaa.config.Constants.AUTH_USER_NAME;
import static com.icthh.xm.uaa.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.HEADER_PORT;
import static com.icthh.xm.uaa.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static com.icthh.xm.uaa.config.Constants.HEADER_WEBAPP_URL;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PORT;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_PROTOCOL;
import static com.icthh.xm.uaa.config.Constants.REQUEST_CTX_WEB_APP;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_PATTERN;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERR_TENANT_NOT_SUPPLIED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERR_TENANT_SUSPENDED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.TENANT_IS_SUSPENDED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.TENANT_NOT_SUPPLIED;

/**
 * Init context for request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyFilter implements Filter {

    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder requestContextHolder;
    private final TenantListRepository tenantListRepository;
    private final XmAuthenticationContextHolder xmAuthContextHolder;
    private final LepManager lepManager;
    private final LepRequestEnrichmentService lepRequestEnrichmentService;

    private TokenStore tokenStore;

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final TokenExtractor tokenExtractor = new BearerTokenExtractor();

    /**
     * {@inheritDoc}
     */
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

        // read data from token
        OAuth2AccessToken token = readAccessToken(httpRequest);
        String tenantKey = getTenantKey(token, httpRequest);
        String userLogin = getUserLogin(token, httpRequest);

        // init logger
        MdcUtils.putRid(MdcUtils.generateRid() + ":"
            + StringUtils.defaultIfBlank(userLogin, "") + ":"
            + StringUtils.defaultIfBlank(tenantKey, ""));

        //check is request without tenant can be skipped by this filter
        if (isNullTenantKeyAllowed(httpRequest, tenantKey)) {
            chain.doFilter(request, response);
            return;
        }

        if (!verify(httpResponse, tenantKey)) {
            return;
        }

        // init tenant context
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        // init xm request context
        XmPrivilegedRequestContext requestContext = requestContextHolder.getPrivilegedContext();
        requestContext.putValue(REQUEST_CTX_PROTOCOL, httpRequest.getHeader(HEADER_SCHEME));
        requestContext.putValue(REQUEST_CTX_DOMAIN, httpRequest.getHeader(HEADER_DOMAIN));
        requestContext.putValue(REQUEST_CTX_PORT, httpRequest.getHeader(HEADER_PORT));
        requestContext.putValue(REQUEST_CTX_WEB_APP, httpRequest.getHeader(HEADER_WEBAPP_URL));

        // init lep context
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthContextHolder.getContext());
        });

        try {
            ServletRequest enrichedRequest = lepRequestEnrichmentService.enrichRequest(request);
            chain.doFilter(enrichedRequest, response);
        } finally {
            lepManager.endThreadContext();
            requestContext.destroyCurrentContext();
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            MdcUtils.clear();
        }
    }

    @Override
    public void destroy() {
        // no logic here
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no logic here
    }

    private static String getTenantKey(OAuth2AccessToken accessToken, HttpServletRequest request) {
        String tenantKeyValue;
        if (accessToken != null) {
            Object tenantFromToken = accessToken.getAdditionalInformation().get(AUTH_TENANT_KEY);
            tenantKeyValue = tenantFromToken != null ? tenantFromToken.toString() : null;
        } else {
            tenantKeyValue = request.getHeader(HEADER_TENANT);
        }

        // TODO fix tenant key uppercase usage
        return tenantKeyValue != null ? tenantKeyValue.toUpperCase() : null;
    }

    private static String getUserLogin(OAuth2AccessToken accessToken, HttpServletRequest request) {
        Object userName;
        if (accessToken != null) {
            userName = accessToken.getAdditionalInformation().get(AUTH_USER_NAME);
        } else {
            userName = request.getParameter(AUTH_USERNAME);
        }
        return userName != null ? userName.toString() : null;
    }

    private boolean isIgnoredRequest(HttpServletRequest request) {
        return isPatternMatch(request, applicationProperties.getTenantIgnoredPathList());
    }

    private boolean isWhiteListRequest(HttpServletRequest request) {
        return isPatternMatch(request, applicationProperties.getProxyFilterWhiteList());
    }

    private boolean isPatternMatch(HttpServletRequest request, List<String> patterns) {
        String path = request.getServletPath();
        if (patterns != null && path != null) {
            for (String pattern : patterns) {
                if (matcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private OAuth2AccessToken readAccessToken(HttpServletRequest request) {
        Authentication auth = tokenExtractor.extract(request);
        if (auth == null) {
            return null;
        }
        String accessTokenValue = (String) auth.getPrincipal();
        return tokenStore.readAccessToken(accessTokenValue);
    }

    private boolean verify(HttpServletResponse httpResponse, String tenantKey) throws IOException {
        if (StringUtils.isBlank(tenantKey)) {
            log.error("Tenant not supplied");
            sendResponse(httpResponse, String.format(ERROR_PATTERN, ERR_TENANT_NOT_SUPPLIED, TENANT_NOT_SUPPLIED));
            return false;
        } else if (tenantListRepository.getSuspendedTenants().contains(tenantKey.toLowerCase())) {
            log.error("Tenant: {} is suspended", tenantKey);
            sendResponse(httpResponse, String.format(ERROR_PATTERN, ERR_TENANT_SUSPENDED, TENANT_IS_SUSPENDED));
            return false;
        }
        return true;
    }

    private boolean isNullTenantKeyAllowed(HttpServletRequest httpRequest, String tenantKey) {
        if (StringUtils.isBlank(tenantKey) && isWhiteListRequest(httpRequest)) {
            return true;
        }

        return false;
    }

    private void sendResponse(HttpServletResponse httpResponse, String format) throws IOException {
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpResponse.getWriter().write(format);
        httpResponse.getWriter().flush();
    }

    @Lazy
    @Autowired
    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }
}
