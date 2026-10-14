package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.commons.security.oauth2.OAuth2Properties;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenConverter;

import com.icthh.xm.uaa.security.DomainJwtAccessTokenDetailsPostProcessor;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.icthh.xm.uaa.security.oauth2.idp.XmJwkTokenStore;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.converter.XmJwkVerifyingJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;


/**
 * The {@link UaaAccessTokenConverterConfiguration} class.
 */
@Slf4j
@Configuration
@Import({
    TenantContextConfiguration.class,
    RestTemplateConfiguration.class
})
public class UaaAccessTokenConverterConfiguration {

    private final RestTemplate keyUriRestTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final UaaKeyProvider uaaKeyProvider;
    private final IdpConfigRepository idpConfigRepository;
    private final JwksRepository jwksRepository;
    private final OAuth2Properties oauth2Properties;

    public UaaAccessTokenConverterConfiguration(TenantContextHolder tenantContextHolder,
                                                @Qualifier("loadBalancedRestTemplate") RestTemplate keyUriRestTemplate,
                                                UaaKeyProvider uaaKeyProvider,
                                                IdpConfigRepository idpConfigRepository,
                                                TenantPropertiesService tenantPropertiesService,
                                                JwksRepository jwksRepository, OAuth2Properties oauth2Properties) {
        this.tenantContextHolder = tenantContextHolder;
        this.keyUriRestTemplate = keyUriRestTemplate;
        this.uaaKeyProvider = uaaKeyProvider;
        this.idpConfigRepository = idpConfigRepository;
        this.jwksRepository = jwksRepository;
        this.oauth2Properties = oauth2Properties;
    }

    /**
     * This bean generates an token enhancer, which manages the exchange between JWT access tokens and Authentication
     * in both directions.
     *
     * @return an access token converter configured with the authorization server's public/private keys
     */
    @Bean
    @SneakyThrows
    public JwtAccessTokenConverter jwtAccessTokenConverter(DomainJwtAccessTokenDetailsPostProcessor tokenDetailsProcessor,
                                                           TenantPropertiesService tenantPropertiesService) {

        // get public key
        final PublicKey publicKey = getKeyFromConfigServer(keyUriRestTemplate);

        // get private key
        final PrivateKey privateKey = uaaKeyProvider.getPrivateKey();

        // build key pair
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        DomainJwtAccessTokenConverter accessTokenConverter = new DomainJwtAccessTokenConverter(tenantContextHolder,
            tenantPropertiesService, tokenDetailsProcessor);
        accessTokenConverter.setKeyPair(keyPair);
        return accessTokenConverter;
    }

    private PublicKey getKeyFromConfigServer(RestTemplate keyUriRestTemplate)
        throws CertificateException, IOException {
        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        String tokenEndpointUrl = oauth2Properties.getSignatureVerification().getPublicKeyEndpointUri();

        String content = keyUriRestTemplate
            .exchange(tokenEndpointUrl, HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty public key from config.");
        }

        try (InputStream tokenKeyInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory factory = CertificateFactory.getInstance(Constants.CERTIFICATE);
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(tokenKeyInputStream);
            return certificate.getPublicKey();
        }
    }

    /**
     * Apply the token converter (and enhancer) for token store.
     */
    @Bean
    @Primary
    public JwtTokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) throws Exception {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    @Bean
    public XmJwkTokenStore jwkTokenStore() {

        XmJwkDefinitionSource xmJwkDefinitionSource = new XmJwkDefinitionSource(jwksRepository);

        XmJwkVerifyingJwtAccessTokenConverter jwkVerifyingJwtAccessTokenConverter =
            new XmJwkVerifyingJwtAccessTokenConverter(xmJwkDefinitionSource, idpConfigRepository);

        return new XmJwkTokenStore(jwkVerifyingJwtAccessTokenConverter);
    }

}
