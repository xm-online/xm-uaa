package com.icthh.xm.uaa.security.oauth2.idp.source.loaders;

import com.icthh.xm.uaa.security.oauth2.idp.source.DefinitionSourceLoader;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

/**
 * Class from loading JWKS public keys from XM storage.
 */
public class LocalStorageDefinitionSourceLoader implements DefinitionSourceLoader {

    private final RestTemplate loadBalancedRestTemplate;

    public LocalStorageDefinitionSourceLoader(RestTemplate loadBalancedRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }

    @SneakyThrows
    @Override
    public List<InputStream> retrieveRawPublicKeysDefinition(Map<String, Object> params) {

        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        String publicKeyEndpointUri = "http://config/api/jwks";//TODO think how to get it
        String content = loadBalancedRestTemplate
            .exchange(publicKeyEndpointUri, HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty public key from config.");
        }
        InputStream jwkSetSource = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return List.of(jwkSetSource);
    }

}
