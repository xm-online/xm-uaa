package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.commons.XmPrivilegedRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.service.LepRequestEnrichmentService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.HEADER_PORT;
import static com.icthh.xm.uaa.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_PATTERN;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERR_TENANT_NOT_SUPPLIED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERR_TENANT_SUSPENDED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.TENANT_IS_SUSPENDED;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.TENANT_NOT_SUPPLIED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProxyFilterUnitTest {

    private static final String API_IGNORE = "/api/ignore";
    private static final String API_WHITE_LIST = "/api/white-list";
    private static final String USER_NAME = "testUser";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private ApplicationProperties properties;
    @Mock
    private PrintWriter writer;
    @Mock
    private TenantListRepository tenantListRepository;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private XmRequestContextHolder xmRequestContextHolder;
    @Mock
    private XmAuthenticationContextHolder xmAuthContextHolder;
    @Mock
    private LepManagementService lepManager;
    @Mock
    private TokenStore tokenStore;
    @Mock
    LepRequestEnrichmentService lepRequestEnrichmentService;

    @InjectMocks
    private ProxyFilter filter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(request.getHeader(HEADER_SCHEME)).thenReturn("http");
        when(request.getHeader(HEADER_DOMAIN)).thenReturn("localhost");
        when(request.getHeader(HEADER_PORT)).thenReturn("8080");
        when(request.getHeader(HEADER_TENANT)).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(request.getHeaders("Authorization")).thenReturn(
            Collections.enumeration(Collections.singleton("Bearer token")));
        when(properties.getTenantIgnoredPathList()).thenReturn(Collections.singletonList(API_IGNORE));
        when(properties.getProxyFilterWhiteList()).thenReturn(Collections.singletonList(API_WHITE_LIST));
        when(response.getWriter()).thenReturn(writer);
        when(tenantListRepository.getSuspendedTenants()).thenReturn(Collections.emptySet());
        PrivilegedTenantContext privilegedTenantContext = mock(PrivilegedTenantContext.class);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(lepRequestEnrichmentService.enrichRequest(request)).thenReturn(request);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_NAME);

        filter.setTokenStore(tokenStore);
    }

    @After
    public void after() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testDoFilterSuccessTenantFromToken() throws Exception {
        PrivilegedTenantContext privilegedTenantContext = mock(PrivilegedTenantContext.class);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);

        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        when(token.getAdditionalInformation()).thenReturn(
            Collections.singletonMap(AUTH_TENANT_KEY, DEFAULT_TENANT_KEY_VALUE));
        when(tokenStore.readAccessToken(any())).thenReturn(token);

        XmPrivilegedRequestContext privilegedRequestContext = mock(XmPrivilegedRequestContext.class);
        when(xmRequestContextHolder.getPrivilegedContext()).thenReturn(privilegedRequestContext);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, times(0)).getHeader(HEADER_TENANT);
        verify(privilegedTenantContext, times(1))
            .setTenant(eq(TenantContextUtils.buildTenant(DEFAULT_TENANT_KEY_VALUE)));
        verify(privilegedTenantContext, times(1)).destroyCurrentContext();
        verify(lepRequestEnrichmentService).enrichRequest(request);
    }

    @Test
    public void testIgnoreRequest() throws Exception {
        when(request.getServletPath()).thenReturn(API_IGNORE);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request).getServletPath();
        verifyNoMoreInteractions(request, response);
        verify(lepRequestEnrichmentService, times(0)).enrichRequest(request);
    }

    @Test
    public void testTenantNotSet() throws Exception {
        when(request.getHeader(HEADER_TENANT)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response, times(2)).getWriter();
        verify(writer).write(String.format(ERROR_PATTERN, ERR_TENANT_NOT_SUPPLIED, TENANT_NOT_SUPPLIED));
        verify(writer).flush();
        verify(lepRequestEnrichmentService, times(0)).enrichRequest(request);
    }

    @Test
    public void testTenantNotSetWithProxyWhiteList() throws Exception {
        when(request.getServletPath()).thenReturn(API_WHITE_LIST);
        when(request.getHeader(HEADER_TENANT)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(lepRequestEnrichmentService, times(0)).enrichRequest(request);
    }

    @Test
    public void testTenantSuspended() throws Exception {
        Set<String> tenants = new HashSet<>();
        tenants.add(DEFAULT_TENANT_KEY_VALUE.toLowerCase());
        when(tenantListRepository.getSuspendedTenants()).thenReturn(tenants);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response, times(2)).getWriter();
        verify(writer).write(String.format(ERROR_PATTERN, ERR_TENANT_SUSPENDED, TENANT_IS_SUSPENDED));
        verify(writer).flush();
        verify(lepRequestEnrichmentService, times(0)).enrichRequest(request);
    }
}
