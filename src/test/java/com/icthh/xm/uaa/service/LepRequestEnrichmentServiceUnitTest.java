package com.icthh.xm.uaa.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.UaaTestConstants.DEFAULT_TENANT_KEY_VALUE;
import static com.icthh.xm.uaa.config.Constants.AUTH_USERNAME;
import static com.icthh.xm.uaa.config.Constants.HEADER_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class LepRequestEnrichmentServiceUnitTest {

    private static final String USER_PARAM_VALUE = "user";
    private static final String CUSTOM_HEADER_VALUE = "CUSTOM_HEADER_VALUE";
    private static final String CUSTOM_HEADER = "CUSTOM_HEADER";
    private static final String CUSTOM_PARAMETER = "CUSTOM_PARAMETER";
    private static final String CUSTOM_PARAMETER_VALUE = "CUSTOM_PARAMETER_VALUE";

    @Mock
    private HttpServletRequest request;

    @Spy
    private LepRequestEnrichmentService lepRequestEnrichmentService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        lepRequestEnrichmentService.setSelf(lepRequestEnrichmentService);
    }

    @Test
    public void testRequestInstanceWasUnchangedForEmptyParameters() throws Exception {
        when(lepRequestEnrichmentService.getCustomHeaders(request)).thenReturn(Collections.emptyMap());
        when(lepRequestEnrichmentService.getCustomParameters(request)).thenReturn(Collections.emptyMap());

        ServletRequest enrichedRequest = lepRequestEnrichmentService.enrichRequest(request);

        assertThat(enrichedRequest).isEqualTo(request);
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
        when(lepRequestEnrichmentService.getCustomHeaders(request)).thenReturn(customHeaders);

        Map<String, String[]> customParameters = new HashMap<>();
        customParameters.put(CUSTOM_PARAMETER, new String[]{CUSTOM_PARAMETER_VALUE});
        when(lepRequestEnrichmentService.getCustomParameters(request)).thenReturn(customParameters);

        ServletRequest enrichedRequest = lepRequestEnrichmentService.enrichRequest(request);

        assertThat(enrichedRequest).isInstanceOf(HttpServletRequest.class);

        HttpServletRequest httpEnrichedRequest = (HttpServletRequest) enrichedRequest;

        assertThat(httpEnrichedRequest.getHeader(CUSTOM_HEADER)).isEqualTo(CUSTOM_HEADER_VALUE);
        assertThat(httpEnrichedRequest.getHeader(HEADER_TENANT)).isEqualTo(DEFAULT_TENANT_KEY_VALUE);
        assertThat(Collections.list(httpEnrichedRequest.getHeaders(CUSTOM_HEADER))).containsOnly(CUSTOM_HEADER_VALUE);
        assertThat(Collections.list(httpEnrichedRequest.getHeaderNames())).containsOnly(CUSTOM_HEADER, HEADER_TENANT);

        assertThat(httpEnrichedRequest.getParameter(CUSTOM_PARAMETER)).isEqualTo(CUSTOM_PARAMETER_VALUE);
        assertThat(Collections.list(httpEnrichedRequest.getParameterNames())).containsOnly(CUSTOM_PARAMETER, AUTH_USERNAME);
        assertThat(httpEnrichedRequest.getParameterValues(CUSTOM_PARAMETER)).containsOnly(CUSTOM_PARAMETER_VALUE);
        assertThat(httpEnrichedRequest.getParameterValues(AUTH_USERNAME)).containsOnly(USER_PARAM_VALUE);
        assertThat(httpEnrichedRequest.getParameterMap()).containsKeys(CUSTOM_PARAMETER, AUTH_USERNAME);
    }
}
