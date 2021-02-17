package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenConverter;

import com.icthh.xm.uaa.security.DomainJwtAccessTokenDetailsPostProcessor;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import com.icthh.xm.uaa.security.oauth2.idp.XmJwkTokenStore;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.converter.XmJwkVerifyingJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
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
    private final ApplicationProperties applicationProperties;
    private final IdpConfigRepository idpConfigRepository;
    private final TenantPropertiesService tenantPropertiesService;
    private final JwksRepository jwksRepository;

    public UaaAccessTokenConverterConfiguration(TenantContextHolder tenantContextHolder,
                                                @Qualifier("loadBalancedRestTemplate") RestTemplate keyUriRestTemplate,
                                                ApplicationProperties applicationProperties,
                                                IdpConfigRepository idpConfigRepository,
                                                TenantPropertiesService tenantPropertiesService,
                                                JwksRepository jwksRepository) {
        this.tenantContextHolder = tenantContextHolder;
        this.keyUriRestTemplate = keyUriRestTemplate;
        this.applicationProperties = applicationProperties;
        this.idpConfigRepository = idpConfigRepository;
        this.tenantPropertiesService = tenantPropertiesService;
        this.jwksRepository = jwksRepository;
    }

    /**
     * This bean generates an token enhancer, which manages the exchange between JWT access tokens and Authentication
     * in both directions.
     *
     * @return an access token converter configured with the authorization server's public/private keys
     */
    @Bean
    @SneakyThrows
    public JwtAccessTokenConverter jwtAccessTokenConverter(DomainJwtAccessTokenDetailsPostProcessor tokenDetailsProcessor) {

        // get public key
        final PublicKey publicKey = getKeyFromConfigServer(keyUriRestTemplate);

        // get private key
        final PrivateKey privateKey = getPrivateKey();

        // build key pair
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        DomainJwtAccessTokenConverter accessTokenConverter = new DomainJwtAccessTokenConverter(tenantContextHolder,
            tokenDetailsProcessor);
        accessTokenConverter.setKeyPair(keyPair);
        return accessTokenConverter;
    }

    private static PublicKey getKeyFromConfigServer(RestTemplate keyUriRestTemplate)
        throws CertificateException, IOException {
        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        String content = keyUriRestTemplate
            .exchange("http://config/api/token_key", HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty public key from config.");
        }

        try (InputStream tokenKeyInputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory factory = CertificateFactory.getInstance(Constants.CERTIFICATE);
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(tokenKeyInputStream);
            return certificate.getPublicKey();
        }
    }

    private PrivateKey getPrivateKey() throws IOException, KeyStoreException, CertificateException,
        NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeySpecException {
        final String privateKey = applicationProperties.getPrivateKey();
        if (!StringUtils.isEmpty(privateKey)) {
            log.info("Key was loaded from memory by application property: application.private-key");

            return initPrivateKey(Base64.decode(applicationProperties.getPrivateKey().getBytes(StandardCharsets.UTF_8)));
        } else {
            log.info("Keystore location {}", applicationProperties.getKeystoreFile());
            try (InputStream stream = new ClassPathResource(applicationProperties.getKeystoreFile()).exists()
                ? new ClassPathResource(applicationProperties.getKeystoreFile()).getInputStream()
                : new FileInputStream(applicationProperties.getKeystoreFile())) {
                return initPrivateKeyFromKeystore(stream);
            }
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

    private PrivateKey initPrivateKeyFromKeystore(InputStream stream) throws KeyStoreException, UnrecoverableKeyException,
        NoSuchAlgorithmException, IOException, CertificateException {
        KeyStore store = KeyStore.getInstance(Constants.KEYSTORE_TYPE);
        store.load(stream, applicationProperties.getKeystorePassword().toCharArray());
        return (PrivateKey) store.getKey(applicationProperties.getKeystoreKeyName(),
            applicationProperties.getKeystorePassword().toCharArray());
    }

    private PrivateKey initPrivateKey(final byte[] encodedPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        return factory.generatePrivate(keySpec);
    }

    @Bean
    public XmJwkTokenStore jwkTokenStore() {

        XmJwkDefinitionSource xmJwkDefinitionSource = new XmJwkDefinitionSource(idpConfigRepository, jwksRepository);

        XmJwkVerifyingJwtAccessTokenConverter jwkVerifyingJwtAccessTokenConverter =
            new XmJwkVerifyingJwtAccessTokenConverter(xmJwkDefinitionSource, idpConfigRepository);

        return new XmJwkTokenStore(jwkVerifyingJwtAccessTokenConverter);
    }

}
