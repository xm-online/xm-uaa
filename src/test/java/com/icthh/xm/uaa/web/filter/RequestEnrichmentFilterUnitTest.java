package com.icthh.xm.uaa.web.filter;

import com.icthh.xm.uaa.domain.EnrichedHttpServletRequest;
import com.icthh.xm.uaa.service.RequestEnrichmentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestEnrichmentFilterUnitTest {

    private static final String USER_PARAM_VALUE = "user";
    private static final String CUSTOM_HEADER_VALUE = "CUSTOM_HEADER_VALUE";
    private static final String CUSTOM_HEADER = "CUSTOM_HEADER";
    private static final String CUSTOM_PARAMETER = "CUSTOM_PARAMETER";
    private static final String CUSTOM_PARAMETER_VALUE = "CUSTOM_PARAMETER_VALUE";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Spy
    private RequestEnrichmentService requestEnrichmentService;

    @InjectMocks
    private RequestEnrichmentFilter filter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        requestEnrichmentService.setSelf(requestEnrichmentService);
    }

    @Test
    public void testDoFilterShouldUseTheSameRequestInstanceForCaseWithoutAnyCustomParams() throws Exception {
        when(requestEnrichmentService.getCustomHeaders(request)).thenReturn(Collections.emptyMap());
        when(requestEnrichmentService.getCustomParameters(request)).thenReturn(Collections.emptyMap());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterCustomHeadersAndParams() throws Exception {
        when(request.getHeader(HEADER_TENANT)).thenReturn(DEFAULT_TENANT_KEY_VALUE);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton(HEADER_TENANT)));
        when(request.getHeaders(HEADER_TENANT)).thenReturn(Collections.enumeration(Collections.singleton(DEFAULT_TENANT_KEY_VALUE)));

        when(request.getParameter(AUTH_USERNAME)).thenReturn(USER_PARAM_VALUE);
        when(request.getParameterNames()).thenReturn(Collections.enumeration(Collections.singleton(AUTH_USERNAME)));
        when(request.getParameterValues(AUTH_USERNAME)).thenReturn(new String[]{USER_PARAM_VALUE});

        Map<String, String[]> parametersMap = new HashMap<>();
        parametersMap.put(AUTH_USERNAME, new String[]{USER_PARAM_VALUE});
        when(request.getParameterMap()).thenReturn(parametersMap);


        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put(CUSTOM_HEADER, CUSTOM_HEADER_VALUE);
        when(requestEnrichmentService.getCustomHeaders(request)).thenReturn(customHeaders);

        Map<String, String[]> customParameters = new HashMap<>();
        customParameters.put(CUSTOM_PARAMETER, new String[]{CUSTOM_PARAMETER_VALUE});
        when(requestEnrichmentService.getCustomParameters(request)).thenReturn(customParameters);
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(EnrichedHttpServletRequest.class), eq(response));
        verify(chain).doFilter(requestCaptor.capture(), eq(response));

        assertThat(requestCaptor.getValue().getHeader(CUSTOM_HEADER)).isEqualTo(CUSTOM_HEADER_VALUE);
        assertThat(requestCaptor.getValue().getHeader(HEADER_TENANT)).isEqualTo(DEFAULT_TENANT_KEY_VALUE);
        assertThat(Collections.list(requestCaptor.getValue().getHeaders(CUSTOM_HEADER))).containsOnly(CUSTOM_HEADER_VALUE);
        assertThat(Collections.list(requestCaptor.getValue().getHeaderNames())).containsOnly(CUSTOM_HEADER, HEADER_TENANT);

        assertThat(requestCaptor.getValue().getParameter(CUSTOM_PARAMETER)).isEqualTo(CUSTOM_PARAMETER_VALUE);
        assertThat(Collections.list(requestCaptor.getValue().getParameterNames())).containsOnly(CUSTOM_PARAMETER, AUTH_USERNAME);
        assertThat(requestCaptor.getValue().getParameterValues(CUSTOM_PARAMETER)).containsOnly(CUSTOM_PARAMETER_VALUE);
        assertThat(requestCaptor.getValue().getParameterValues(AUTH_USERNAME)).containsOnly(USER_PARAM_VALUE);
        assertThat(requestCaptor.getValue().getParameterMap()).containsKeys(CUSTOM_PARAMETER, AUTH_USERNAME);
    }
}
