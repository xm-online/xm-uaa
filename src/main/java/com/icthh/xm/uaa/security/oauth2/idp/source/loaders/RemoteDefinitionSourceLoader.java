package com.icthh.xm.uaa.security.oauth2.idp.source.loaders;

import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigContainer;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpPublicConfig;
import com.icthh.xm.uaa.security.oauth2.idp.source.DefinitionSourceLoader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteDefinitionSourceLoader implements DefinitionSourceLoader {

    private final IdpConfigRepository idpConfigRepository;

    public RemoteDefinitionSourceLoader(IdpConfigRepository idpConfigRepository) {
        this.idpConfigRepository = idpConfigRepository;
    }

    @Override
    public List<InputStream> retrieveRawPublicKeysDefinition(Map<String, String> params) {

        String tenantKey = params.get("tenantKey");
        List<URL> jwkSetUrls = loadJwkSetUrls(tenantKey);

        return jwkSetUrls
            .stream()
            .map(jwkSetUrl -> {
                try {
                    return jwkSetUrl.openStream();
                } catch (IOException ex) {
                    throw new JwkException("An I/O error occurred while reading from the JWK Set source: " + ex.getMessage(), ex);
                }
            }).collect(Collectors.toList());
    }

    public List<URL> loadJwkSetUrls(String tenantKey) {
        List<String> jwkSetEndpoints = getJwkSetEndpoints(tenantKey);

        return jwkSetEndpoints
            .stream()
            .map(jwkSetEndpoint -> {
                try {
                    return new URL(jwkSetEndpoint);
                } catch (MalformedURLException ex) {
                    throw new IllegalArgumentException("Invalid JWK Set URL: " + ex.getMessage(), ex);
                }
            }).collect(Collectors.toList());
    }

    private List<String> getJwkSetEndpoints(String tenantKey) {
        Map<String, IdpConfigContainer> idpClientConfig = idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey);

        return idpClientConfig.values()
            .stream()
            .map(IdpConfigContainer::getIdpPublicClientConfig)
            .map(IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig::getJwksEndpoint)
            .map(IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig.BaseEndpoint::getUri)
            .filter(jwksUri -> !StringUtils.isEmpty(jwksUri))
            .collect(Collectors.toList());
    }
}
