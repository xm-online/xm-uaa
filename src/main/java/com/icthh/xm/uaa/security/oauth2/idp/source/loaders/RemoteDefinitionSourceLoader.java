package com.icthh.xm.uaa.security.oauth2.idp.source.loaders;

import com.icthh.xm.uaa.security.oauth2.idp.source.DefinitionSourceLoader;
import lombok.NoArgsConstructor;
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

        List<URL> jwkSetUrls = loadJwkSetUrls(getJwkSetEndpoints());

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

    /**
     * stubbed until {@link LocalStorageDefinitionSourceLoader} will not be implemented.
     */

    private List<String> getJwkSetEndpoints() {
        return List.of("https://ticino-dev-co.eu.auth0.com/.well-known/jwks.json");
    }
}
