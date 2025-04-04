package com.icthh.xm.uaa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.LepTextConfiguration;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    LepTextConfiguration.class,
    UaaApp.class,
    XmOverrideConfiguration.class
})
@Transactional
public class UaaAccessTokenConverterConfigurationIntTest {

    private static final String TENANT = "XM";
    private static final String CLIENT = "testClient";
    private static final String LOGIN = "testLogin";
    private static final String ROLE = "testRole";

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private AuthorizationServerTokenServices tokenServices;
    @Value("${:classpath:config/tenants/XM/uaa/uaa.yml}")
    private Resource spec;
    @Autowired
    private LepManager lepManager;
    @MockBean
    private TenantPropertiesService tenantPropertiesService;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;


    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @BeforeTransaction
    public void BeforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
    }

    @Before
    @SneakyThrows
    public void setup() {
        MockitoAnnotations.initMocks(this);
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        String conf = StreamUtils.copyToString(spec.getInputStream(), Charset.defaultCharset());
        TenantProperties tenantProperties = mapper.readValue(conf, TenantProperties.class);
        when(tenantPropertiesService.getTenantProps()).thenReturn(tenantProperties);

    }


    @Test
    @SneakyThrows
    public void testPrivateKeyFromMemory() {
        OAuth2AccessToken accessToken = tokenServices.createAccessToken(createAuthentication(LOGIN, TENANT, ROLE));
        Assert.assertNotNull(accessToken.getValue());
        RsaVerifier rsaVerifier = new RsaVerifier(jwtAccessTokenConverter.getKey().get("value"));
        JwtHelper.decodeAndVerify(accessToken.getValue(), rsaVerifier);
    }

    private OAuth2Authentication createAuthentication(String username, String tenant, String role, Map<String, String> additionalDetails) {
        DomainUserDetails principal = new DomainUserDetails(username,
            "test",
            Collections.singletonList(new SimpleGrantedAuthority(role)),
            tenant,
            "userKey",
            false,
            null,
            null,
            false,
            null);
        principal.getAdditionalDetails().putAll(additionalDetails);

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
            principal.getAuthorities());

        // Create the authorization request and OAuth2Authentication object
        OAuth2Request authRequest = new OAuth2Request(null, CLIENT, null, true, null, null, null, null,
            null);
        return new OAuth2Authentication(authRequest, authentication);
    }

    private OAuth2Authentication createAuthentication(String username, String tenant, String role) {
        return createAuthentication(username, tenant, role, Collections.emptyMap());
    }
}
