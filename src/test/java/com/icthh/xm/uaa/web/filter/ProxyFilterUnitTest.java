package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.config.Constants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.icthh.xm.uaa.config.Constants.AUTH_TENANT_KEY;
import static com.icthh.xm.uaa.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.uaa.config.Constants.HEADER_PORT;
import static com.icthh.xm.uaa.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProxyFilterUnitTest {

    private static final String API_IGNORE = "/api/ignore";

    @InjectMocks
    private ProxyFilter filter;
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
    private OAuth2Authentication authentication;
    @Mock
    private TenantListRepository tenantListRepository;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(request.getHeader(HEADER_SCHEME)).thenReturn("http");
        when(request.getHeader(HEADER_DOMAIN)).thenReturn("localhost");
        when(request.getHeader(HEADER_PORT)).thenReturn("8080");
        when(request.getHeader(HEADER_TENANT)).thenReturn(Constants.DEFAULT_TENANT);
        when(properties.getTenantIgnoredPathList()).thenReturn(Collections.singletonList(API_IGNORE));
        when(response.getWriter()).thenReturn(writer);
        when(tenantListRepository.getSuspendedTenants()).thenReturn(Collections.emptySet());

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testDoFilterSuccessTenantFromHeader() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterSuccessTenantFromToken() throws Exception {
        OAuth2AuthenticationDetails authDetails = mock(OAuth2AuthenticationDetails.class);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getDetails()).thenReturn(authDetails);
        when(authDetails.getDecodedDetails()).thenReturn(Collections.singletonMap(AUTH_TENANT_KEY,
            Constants.DEFAULT_TENANT));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, times(0)).getHeader(HEADER_TENANT);
    }

    @Test
    public void testIgnoreRequest() throws Exception {
        when(request.getServletPath()).thenReturn(API_IGNORE);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request).getServletPath();
        verifyNoMoreInteractions(request, response);
    }

    @Test
    public void testTenantNotSet() throws Exception {
        when(authentication.getDetails()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response, times(2)).getWriter();
        verify(writer).write("{\"error\": \"No tenant supplied\"}");
        verify(writer).flush();
    }

    @Test
    public void testTenantSuspended() throws Exception {
        Set<String> tenants = new HashSet<>();
        tenants.add(Constants.DEFAULT_TENANT.toLowerCase());
        when(tenantListRepository.getSuspendedTenants()).thenReturn(tenants);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response, times(2)).getWriter();
        verify(writer).write("{\"error\": \"Tenant is suspended\"}");
        verify(writer).flush();
    }
}
