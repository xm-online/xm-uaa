package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * The {@link UaaAccessTokenConverterConfiguration} class.
 */
@Configuration
@Import( {
    TenantContextConfiguration.class,
    RestTemplateConfiguration.class
})
public class UaaAccessTokenConverterConfiguration {

    private final RestTemplate keyUriRestTemplate;
    private final TenantContextHolder tenantContextHolder;

    public UaaAccessTokenConverterConfiguration(TenantContextHolder tenantContextHolder,
                                                @Qualifier("loadBalancedRestTemplate") RestTemplate keyUriRestTemplate) {
        this.tenantContextHolder = tenantContextHolder;
        this.keyUriRestTemplate = keyUriRestTemplate;
    }

    /**
     * This bean generates an token enhancer, which manages the exchange between JWT acces tokens and Authentication
     * in both directions.
     *
     * @return an access token converter configured with the authorization server's public/private keys
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() throws IOException, KeyStoreException,
        CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        // get public key
        final PublicKey publicKey = getKeyFromConfigServer(keyUriRestTemplate);

        // get private key
        InputStream stream = new ClassPathResource(Constants.KEYSTORE_FILE).getInputStream();
        KeyStore store = KeyStore.getInstance(Constants.KEYSTORE_TYPE);
        store.load(stream, "password".toCharArray());
        final PrivateKey privateKey = (PrivateKey) store.getKey("selfsigned", "password".toCharArray());

        // build key pair
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        DomainJwtAccessTokenConverter accessTokenConverter = new DomainJwtAccessTokenConverter(tenantContextHolder);
        accessTokenConverter.setKeyPair(keyPair);
        return accessTokenConverter;
    }

    private static PublicKey getKeyFromConfigServer(RestTemplate keyUriRestTemplate) throws CertificateException {
        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        String content = keyUriRestTemplate
            .exchange("http://config/api/token_key", HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty public key from config.");
        }

        InputStream tokenKeyInputStream = new ByteArrayInputStream(content.getBytes());

        CertificateFactory factory = CertificateFactory.getInstance(Constants.CERTIFICATE);
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(tokenKeyInputStream);
        return certificate.getPublicKey();
    }

    /**
     * Apply the token converter (and enhancer) for token store.
     */
    @Bean
    public JwtTokenStore tokenStore() throws Exception {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

}
