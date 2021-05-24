package com.icthh.xm.uaa.security.oauth2.idp.source;

import com.icthh.xm.commons.repository.JwksRepository;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.EllipticCurveJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkDefinitionSource;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.RsaJwkDefinition;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Added retrieving {@link JwksRepository#getTenantIdpJwks()} (String)} cached jwks from tenant configuration.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class XmJwkDefinitionSource extends JwkDefinitionSource {
    private final JwksRepository jwksRepository;

    public XmJwkDefinitionSource(JwksRepository jwksRepository) {
        this.jwksRepository = jwksRepository;
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     * If the JWK definition is not available in the internal cache
     * then {@link JwksRepository#getTenantIdpJwks()} (String)} }
     * will be called (to re-load the cache) and then followed-up with a second attempt to locate the JWK definition.
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link JwkDefinitionHolder} or null if not found
     */
    @Override
    public JwkDefinitionHolder getDefinitionLoadIfNecessary(String keyId) {
        JwkDefinitionHolder result = this.getDefinition(keyId);
        if (result != null) {
            return result;
        }
        synchronized (getJwkDefinitions()) {
            result = this.getDefinition(keyId);
            if (result != null) {
                return result;
            }

            Map<String, JwkDefinitionHolder> newJwkDefinitions = updateJwkDefinitionHolders();

            getJwkDefinitions().clear();
            getJwkDefinitions().putAll(newJwkDefinitions);
            return this.getDefinition(keyId);
        }
    }

    private Map<String, JwkDefinitionHolder> updateJwkDefinitionHolders() {
        Map<String, String> idpJwksByTenantKey = jwksRepository.getTenantIdpJwks();

        List<ByteArrayInputStream> publicKeysRawDefinition = idpJwksByTenantKey.values()
            .stream()
            .map(content -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
            .collect(Collectors.toList());

        Map<String, JwkDefinitionHolder> newJwkDefinitions = new LinkedHashMap<>();
        publicKeysRawDefinition.forEach(rawDefinition -> newJwkDefinitions.putAll(buildJwkDefinitions(rawDefinition)));

        return newJwkDefinitions;
    }

    /**
     * Returns the JWK definition matching the provided keyId (&quot;kid&quot;).
     *
     * @param keyId the Key ID (&quot;kid&quot;)
     * @return the matching {@link JwkDefinitionHolder} or null if not found
     */
    private JwkDefinitionHolder getDefinition(String keyId) {
        return getJwkDefinitions().get(keyId);
    }

    private static Map<String, JwkDefinitionHolder> buildJwkDefinitions(InputStream jwkSetSource) {
        Set<JwkDefinition> jwkDefinitionSet = getJwkSetConverter().convert(jwkSetSource);

        Map<String, JwkDefinitionHolder> jwkDefinitions = new LinkedHashMap<>();

        for (JwkDefinition jwkDefinition : jwkDefinitionSet) {
            if (JwkDefinition.KeyType.RSA.equals(jwkDefinition.getKeyType())) {
                jwkDefinitions.put(jwkDefinition.getKeyId(),
                    new JwkDefinitionHolder(jwkDefinition, createRsaVerifier((RsaJwkDefinition) jwkDefinition)));
            } else if (JwkDefinition.KeyType.EC.equals(jwkDefinition.getKeyType())) {
                jwkDefinitions.put(jwkDefinition.getKeyId(),
                    new JwkDefinitionHolder(jwkDefinition, createEcVerifier((EllipticCurveJwkDefinition) jwkDefinition)));
            }
        }

        return jwkDefinitions;
    }

}
