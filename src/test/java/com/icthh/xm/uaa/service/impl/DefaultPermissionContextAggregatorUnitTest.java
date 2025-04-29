package com.icthh.xm.uaa.service.impl;

import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.dto.PermissionContextDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class DefaultPermissionContextAggregatorUnitTest {

    private static final String TOKEN = UUID.randomUUID().toString();
    private static final String USER_KEY = UUID.randomUUID().toString();
    private static final Map<String, Object> CONTEXT_MAP = Map.of(
        "key_str", "value1",
        "key_int", 111,
        "key_bool", true,
        "key_double", 12.54,
        "key_long", 101L
    );

    @Mock
    private TenantPropertiesService tenantPropertiesService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private TenantProperties.ContextPermission contextPermission;

    private DefaultPermissionContextAggregator service;

    private final String permissionContextPathPattern = "api/permissions/context";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);
        when(tenantProperties.getContextPermission()).thenReturn(contextPermission);
        when(contextPermission.getServices()).thenReturn(List.of("serviceA", "serviceB"));

        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setPermissionContextPathPattern(permissionContextPathPattern);

        service = new DefaultPermissionContextAggregator(applicationProperties, tenantPropertiesService, restTemplate);

        // Mock SecurityContextHolder
        SecurityContext securityContext = mock(SecurityContext.class);
        OAuth2Authentication auth = mock(OAuth2Authentication.class);
        OAuth2AuthenticationDetails authDetails = mock(OAuth2AuthenticationDetails.class);

        when(securityContext.getAuthentication()).thenReturn(auth);
        when(auth.getDetails()).thenReturn(authDetails);
        when(authDetails.getTokenValue()).thenReturn(TOKEN);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void loadPermissionsFromServices_ShouldReturnResultFromEachService() {
        PermissionContextDto dtoA = buildDefaultContextDto(List.of("PERMISSION_A1", "PERMISSION_A2"));
        PermissionContextDto dtoB = buildDefaultContextDto(List.of("PERMISSION_B1", "PERMISSION_B2"));

        when(restTemplate.exchange(
            eq("http://serviceA/" + permissionContextPathPattern + "?userKey=" + USER_KEY),
            eq(HttpMethod.GET),
            argThat(new TokenHeaderExists()),
            eq(PermissionContextDto.class)
        )).thenReturn(new ResponseEntity<>(dtoA, HttpStatus.OK));

        when(restTemplate.exchange(
            eq("http://serviceB/" + permissionContextPathPattern + "?userKey=" + USER_KEY),
            eq(HttpMethod.GET),
            argThat(new TokenHeaderExists()),
            eq(PermissionContextDto.class)
        )).thenReturn(new ResponseEntity<>(dtoB, HttpStatus.OK));

        Map<String, PermissionContextDto> result = service.loadPermissionsFromServices(USER_KEY);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("serviceA"));
        assertTrue(result.containsKey("serviceB"));
        assertEquals(dtoA, result.get("serviceA"));
        assertEquals(dtoB, result.get("serviceB"));

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(PermissionContextDto.class));
    }

    @Test
    public void getContextFromService_shouldReturnEmptyContext() {
        when(restTemplate.exchange(
            eq("http://serviceA/" + permissionContextPathPattern + "?userKey=" + USER_KEY),
            eq(HttpMethod.GET),
            argThat(new TokenHeaderExists()),
            eq(PermissionContextDto.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        when(restTemplate.exchange(
            eq("http://serviceB/" + permissionContextPathPattern + "?userKey=" + USER_KEY),
            eq(HttpMethod.GET),
            argThat(new TokenHeaderExists()),
            eq(PermissionContextDto.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        Map<String, PermissionContextDto> result = service.loadPermissionsFromServices(USER_KEY);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("serviceA"));
        assertTrue(result.containsKey("serviceB"));
        assertEquals(new PermissionContextDto(), result.get("serviceA"));
        assertEquals(new PermissionContextDto(), result.get("serviceB"));

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(PermissionContextDto.class));
    }

    private PermissionContextDto buildDefaultContextDto(List<String> permissions) {
        PermissionContextDto dto = new PermissionContextDto();
        dto.setPermissions(permissions);
        dto.setCtx(CONTEXT_MAP);
        return dto;
    }

    static class TokenHeaderExists implements ArgumentMatcher<HttpEntity> {

        @Override
        public boolean matches(HttpEntity actual) {
            assertEquals("Bearer " + TOKEN, actual.getHeaders().get("Authorization").iterator().next());
            return true;
        }
    }
}
