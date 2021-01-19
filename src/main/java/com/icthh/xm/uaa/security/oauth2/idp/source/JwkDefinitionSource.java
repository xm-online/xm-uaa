package com.icthh.xm.uaa.security.oauth2.idp.source;

import com.icthh.xm.uaa.security.oauth2.idp.converter.CustomJwkSetConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomRsaJwkDefinition;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.jwt.codec.Codecs;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class JwkDefinitionSource implements DefinitionSource {

    private final RestTemplate loadBalancedRestTemplate;
    private final Map<String, JwkDefinitionHolder> jwkDefinitions = new ConcurrentHashMap<>();
    private static final CustomJwkSetConverter customJwkSetConverter = new CustomJwkSetConverter();
    private List<URL> jwkSetUrls;
    private boolean retrieveFromRemoteConfig;


    public JwkDefinitionSource(RestTemplate loadBalancedRestTemplate, boolean retrieveFromRemoteConfig) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.retrieveFromRemoteConfig = retrieveFromRemoteConfig;
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     * If the JWK definition is not available in the internal cache then {@link #loadJwkDefinitionsFromPublicStorage(URL)}
     * will be called (to re-load the cache) and then followed-up with a second attempt to locate the JWK definition.
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link CustomJwkDefinition} or null if not found
     */
    public JwkDefinitionHolder getDefinitionLoadIfNecessary(String keyId) {
        JwkDefinitionHolder result = this.getDefinition(keyId);
        if (result != null) {
            return result;
        }
        synchronized (this.jwkDefinitions) {
            result = this.getDefinition(keyId);
            if (result != null) {
                return result;
            }
            Map<String, JwkDefinitionHolder> newJwkDefinitions = new LinkedHashMap<>();
            if (this.retrieveFromRemoteConfig) {
                updateJwkSetUrlsFromPublicConfig();
                jwkSetUrls.forEach(jwkSetUrl ->
                    newJwkDefinitions.putAll(loadJwkDefinitionsFromPublicStorage(jwkSetUrl)));
            } else {
                newJwkDefinitions.putAll(loadJwkDefinitionsFromConfig("http://config/api/jwks"));
            }

            this.jwkDefinitions.clear();
            this.jwkDefinitions.putAll(newJwkDefinitions);
            return this.getDefinition(keyId);
        }
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link CustomJwkDefinition} or null if not found
     */
    private JwkDefinitionHolder getDefinition(String keyId) {
        return this.jwkDefinitions.get(keyId);
    }

    //TODO retrieve jwkSetEndpoints from public config
    private void updateJwkSetUrlsFromPublicConfig() {
        String wellKnownPath = "https://ticino-dev-co.eu.auth0.com/.well-known/jwks.json";
        List<String> jwkSetEndpoints = Collections.singletonList(wellKnownPath);
        this.jwkSetUrls = new ArrayList<>();
        for (String jwkSetUrl : jwkSetEndpoints) {
            try {
                this.jwkSetUrls.add(new URL(jwkSetUrl));
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("Invalid JWK Set URL: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Fetches the JWK Set from the provided <code>URL</code> and
     * returns a <code>Map</code> keyed by the JWK keyId (&quot;kid&quot;)
     * and mapped to an association of the {@link CustomJwkDefinition} and {@link SignatureVerifier}.
     * Uses a {@link CustomJwkSetConverter} to convert the JWK Set URL source to a set of {@link CustomJwkDefinition}(s)
     * followed by the instantiation of a {@link SignatureVerifier} which is associated to it's {@link CustomJwkDefinition}.
     *
     * @param jwkSetUrl the JWK Set URL
     * @return a <code>Map</code> keyed by the JWK keyId and mapped to an association of {@link CustomJwkDefinition} and {@link SignatureVerifier}
     * @see CustomJwkSetConverter
     */
    public static Map<String, JwkDefinitionHolder> loadJwkDefinitionsFromPublicStorage(URL jwkSetUrl) {
        InputStream jwkSetSource;
        try {
            jwkSetSource = jwkSetUrl.openStream();
        } catch (IOException ex) {
            throw new JwkException("An I/O error occurred while reading from the JWK Set source: " + ex.getMessage(), ex);
        }

        return buildJwkDefinitions(jwkSetSource);
    }

    /**
     * Fetches the JWK Set from the provided <code>URL</code> and
     * returns a <code>Map</code> keyed by the JWK keyId (&quot;kid&quot;)
     * and mapped to an association of the {@link CustomJwkDefinition} and {@link SignatureVerifier}.
     * Uses a {@link CustomJwkSetConverter} to convert the JWK Set URL source to a set of {@link CustomJwkDefinition}(s)
     * followed by the instantiation of a {@link SignatureVerifier} which is associated to it's {@link CustomJwkDefinition}.
     *
     * @param publicKeyEndpointUri the public key endpoint uri
     * @return a <code>Map</code> keyed by the JWK keyId and mapped to an association of {@link CustomJwkDefinition} and {@link SignatureVerifier}
     * @see CustomJwkSetConverter
     */
    @SneakyThrows
    public Map<String, JwkDefinitionHolder> loadJwkDefinitionsFromConfig(String publicKeyEndpointUri) {

        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        publicKeyEndpointUri = "http://config/api/jwks";
        String content = loadBalancedRestTemplate
            .exchange(publicKeyEndpointUri, HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty public key from config.");
        }

        try (InputStream jwkSetSource = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return buildJwkDefinitions(jwkSetSource);
        } catch (IOException e) {

        }

        return null;
    }

    private static Map<String, JwkDefinitionHolder> buildJwkDefinitions(InputStream jwkSetSource) {
        Set<CustomJwkDefinition> jwkDefinitionSet = customJwkSetConverter.convert(jwkSetSource);

        Map<String, JwkDefinitionHolder> jwkDefinitions = new LinkedHashMap<>();

        for (CustomJwkDefinition jwkDefinition : jwkDefinitionSet) {
            if (CustomJwkDefinition.KeyType.RSA.equals(jwkDefinition.getKeyType())) {
                jwkDefinitions.put(jwkDefinition.getKeyId(),
                    new JwkDefinitionHolder(jwkDefinition, createRsaVerifier((CustomRsaJwkDefinition) jwkDefinition)));
            }
        }

        return jwkDefinitions;
    }

    private static RsaVerifier createRsaVerifier(CustomRsaJwkDefinition rsaDefinition) {
        RsaVerifier result;
        try {
            BigInteger modulus = new BigInteger(1, Codecs.b64UrlDecode(rsaDefinition.getModulus()));
            BigInteger exponent = new BigInteger(1, Codecs.b64UrlDecode(rsaDefinition.getExponent()));

            RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(modulus, exponent));

            if (rsaDefinition.getAlgorithm() != null) {
                result = new RsaVerifier(rsaPublicKey, rsaDefinition.getAlgorithm().standardName());
            } else {
                result = new RsaVerifier(rsaPublicKey);
            }

        } catch (Exception ex) {
            throw new JwkException("An error occurred while creating a RSA Public Key Verifier for " +
                rsaDefinition.getKeyId() + " : " + ex.getMessage(), ex);
        }
        return result;
    }

    public static class JwkDefinitionHolder {
        private final CustomJwkDefinition jwkDefinition;
        private final SignatureVerifier signatureVerifier;

        private JwkDefinitionHolder(CustomJwkDefinition jwkDefinition, SignatureVerifier signatureVerifier) {
            this.jwkDefinition = jwkDefinition;
            this.signatureVerifier = signatureVerifier;
        }

        public CustomJwkDefinition getJwkDefinition() {
            return jwkDefinition;
        }

        public SignatureVerifier getSignatureVerifier() {
            return signatureVerifier;
        }
    }

}
