package com.icthh.xm.uaa.security.oauth2.idp.source.loaders;

import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigContainer;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpPublicConfig;
import com.icthh.xm.uaa.security.oauth2.idp.source.DefinitionSourceLoader;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class from loading JWKS public keys from remote host.
 */
@NoArgsConstructor
public class RemoteDefinitionSourceLoader implements DefinitionSourceLoader {

    @Override
    public List<InputStream> retrieveRawPublicKeysDefinition(Map<String, Object> params) {

        Map<String, IdpConfigContainer> idpClientConfig = (Map<String, IdpConfigContainer>) params.get("clientConfigs");

        List<URL> jwkSetUrls = loadJwkSetUrls(getJwkSetEndpoints(idpClientConfig));

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

    public List<URL> loadJwkSetUrls(List<String> jwkSetEndpoints) {
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

    private List<String> getJwkSetEndpoints(Map<String, IdpConfigContainer> idpClientConfig) {
        return idpClientConfig.values()
            .stream()
            .map(IdpConfigContainer::getIdpPublicClientConfig)
            .map(IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig::getJwksEndpoint)
            .map(IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig.BaseEndpoint::getUri)
            .filter(jwksUri -> !StringUtils.isEmpty(jwksUri))
            .collect(Collectors.toList());
    }
}
