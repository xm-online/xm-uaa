package com.icthh.xm.uaa.security.oauth2.idp.source;

import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.converter.XmJwkSetConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmRsaJwkDefinition;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jwt.codec.Codecs;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import com.icthh.xm.commons.repository.JwksRepository;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class copied from org.springframework.security.oauth2.provider.token.store.jwk.JwkDefinitionSource.
 * Reason: we need custom implementation of JwkDefinitionSource class which impossible to import and override
 * cause it has package private access.
 * <p>
 * What was changed:
 * <ul>
 * <li/>Added IdpConfigRepository, TenantPropertiesService, JwksRepository properties.
 * <li/>Original property jwkSetUrls was removed as redundant.
 * <li/>Original property JwkSetConverter has custom implementation.
 * <li/>Original constructors was removed as redundant.
 * <li/>Original method loadJwkDefinitions was removed as redundant.
 * <li/>Method {@link XmJwkDefinitionSource#getDefinitionLoadIfNecessary(String)} was changed.
 * <li/>Added retrieving {@link JwksRepository#getIdpJwksByTenantKey(String)} cached jwks from tenant configuration.
 * </ul>
 */

@Data
@Slf4j
public class XmJwkDefinitionSource {
    private final IdpConfigRepository idpConfigRepository;
    private final TenantPropertiesService tenantPropertiesService;
    private final JwksRepository jwksRepository;

    private final Map<String, XmJwkDefinitionHolder> jwkDefinitions = new ConcurrentHashMap<>();
    private static final XmJwkSetConverter xmJwkSetConverter = new XmJwkSetConverter();

    public XmJwkDefinitionSource(IdpConfigRepository idpConfigRepository,
                                 TenantPropertiesService tenantPropertiesService,
                                 JwksRepository jwksRepository) {
        this.idpConfigRepository = idpConfigRepository;
        this.tenantPropertiesService = tenantPropertiesService;
        this.jwksRepository = jwksRepository;
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     * If the JWK definition is not available in the internal cache
     * then {@link JwksRepository#getIdpJwksByTenantKey(String)} }
     * will be called (to re-load the cache) and then followed-up with a second attempt to locate the JWK definition.
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link XmJwkDefinitionHolder} or null if not found
     */
    public XmJwkDefinitionHolder getDefinitionLoadIfNecessary(String keyId) {
        XmJwkDefinitionHolder result = this.getDefinition(keyId);
        if (result != null) {
            return result;
        }
        synchronized (this.jwkDefinitions) {
            result = this.getDefinition(keyId);
            if (result != null) {
                return result;
            }

            Map<String, XmJwkDefinitionHolder> newJwkDefinitions = updateJwkDefinitionHolders();

            this.jwkDefinitions.clear();
            this.jwkDefinitions.putAll(newJwkDefinitions);
            return this.getDefinition(keyId);
        }
    }

    private Map<String, XmJwkDefinitionHolder> updateJwkDefinitionHolders() {
        String tenantKey = getTenantKey();

        Map<String, String> idpJwksByTenantKey = jwksRepository.getIdpJwksByTenantKey(tenantKey);
        List<ByteArrayInputStream> publicKeysRawDefinition = idpJwksByTenantKey.values()
            .stream()
            .map(content -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
            .collect(Collectors.toList());

        Map<String, XmJwkDefinitionHolder> newJwkDefinitions = new LinkedHashMap<>();
        publicKeysRawDefinition.forEach(rawDefinition -> newJwkDefinitions.putAll(buildJwkDefinitions(rawDefinition)));

        return newJwkDefinitions;
    }

    private String getTenantKey() {
        return tenantPropertiesService.getTenantContextHolder().getTenantKey();
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link XmJwkDefinition} or null if not found
     */
    private XmJwkDefinitionHolder getDefinition(String keyId) {
        return this.jwkDefinitions.get(keyId);
    }

    private static Map<String, XmJwkDefinitionHolder> buildJwkDefinitions(InputStream jwkSetSource) {
        Set<XmJwkDefinition> jwkDefinitionSet = xmJwkSetConverter.convert(jwkSetSource);

        Map<String, XmJwkDefinitionHolder> jwkDefinitions = new LinkedHashMap<>();

        for (XmJwkDefinition jwkDefinition : jwkDefinitionSet) {
            if (XmJwkDefinition.KeyType.RSA.equals(jwkDefinition.getKeyType())) {
                jwkDefinitions.put(jwkDefinition.getKeyId(),
                    new XmJwkDefinitionHolder(jwkDefinition, createRsaVerifier((XmRsaJwkDefinition) jwkDefinition)));
            }
        }

        return jwkDefinitions;
    }

    private static RsaVerifier createRsaVerifier(XmRsaJwkDefinition rsaDefinition) {
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

    public static class XmJwkDefinitionHolder {
        private final XmJwkDefinition jwkDefinition;
        private final SignatureVerifier signatureVerifier;

        private XmJwkDefinitionHolder(XmJwkDefinition jwkDefinition, SignatureVerifier signatureVerifier) {
            this.jwkDefinition = jwkDefinition;
            this.signatureVerifier = signatureVerifier;
        }

        public XmJwkDefinition getJwkDefinition() {
            return jwkDefinition;
        }

        public SignatureVerifier getSignatureVerifier() {
            return signatureVerifier;
        }
    }

}
