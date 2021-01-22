/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.icthh.xm.uaa.security.oauth2.idp.converter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigContainer;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpPublicConfig;
import com.icthh.xm.uaa.security.oauth2.idp.source.JwkDefinitionSource;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.validation.verifiers.AudienceClaimVerifier;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.IssuerClaimVerifier;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkAttributes.ALGORITHM;
import static com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkAttributes.KEY_ID;


/**
 * A specialized extension of {@link JwtAccessTokenConverter} that is responsible for verifying
 * the JSON Web Signature (JWS) for a JSON Web Token (JWT) using the corresponding JSON Web Key (JWK).
 * This implementation is associated with a {@link JwkDefinitionSource} for looking up
 * the matching {@link CustomJwkDefinition} using the value of the JWT header parameter <b>&quot;kid&quot;</b>.
 * <br>
 * <br>
 * <p>
 * The JWS is verified in the following step sequence:
 * <br>
 * <br>
 * <ol>
 *     <li>Extract the <b>&quot;kid&quot;</b> parameter from the JWT header.</li>
 *     <li>Find the matching {@link CustomJwkDefinition} from the {@link JwkDefinitionSource} with the corresponding <b>&quot;kid&quot;</b> attribute.</li>
 *     <li>Obtain the {@link SignatureVerifier} associated with the {@link CustomJwkDefinition} via the {@link JwkDefinitionSource} and verify the signature.</li>
 * </ol>
 * <br>
 * <b>NOTE:</b> The algorithms currently supported by this implementation are: RS256, RS384 and RS512.
 * <br>
 * <br>
 *
 * <b>NOTE:</b> This {@link JwtAccessTokenConverter} <b>does not</b> support signing JWTs (JWS) and therefore
 * the {@link #encode(OAuth2AccessToken, OAuth2Authentication)} method implementation, if called,
 * will explicitly throw a {@link JwkException} reporting <i>&quot;JWT signing (JWS) is not supported.&quot;</i>.
 * <br>
 * <br>
 *
 * @see JwtAccessTokenConverter
 * @see CustomJwtHeaderConverter
 * @see JwkDefinitionSource
 * @see CustomJwkDefinition
 * @see SignatureVerifier
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7517">JSON Web Key (JWK)</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7519">JSON Web Token (JWT)</a>
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7515">JSON Web Signature (JWS)</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomJwkVerifyingJwtAccessTokenConverter extends JwtAccessTokenConverter {
    private JwkDefinitionSource jwkDefinitionSource;
    private Map<String, Map<String, JwtClaimsSetVerifier>> jwtClaimsSetVerifiers = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;
    private final CustomJwtHeaderConverter customJwtHeaderConverter = new CustomJwtHeaderConverter();
    private final JsonParser jsonParser = JsonParserFactory.create();

    /**
     * Creates a new instance using the provided {@link JwkDefinitionSource}
     * as the primary source for looking up {@link CustomJwkDefinition}(s).
     *
     * @param jwkDefinitionSource the source for {@link CustomJwkDefinition}(s)
     * @param tenantContextHolder tenant context holder
     * @param idpConfigRepository
     */
    public CustomJwkVerifyingJwtAccessTokenConverter(JwkDefinitionSource jwkDefinitionSource,
                                                     TenantContextHolder tenantContextHolder,
                                                     IdpConfigRepository idpConfigRepository) {
        this.jwkDefinitionSource = jwkDefinitionSource;
        this.tenantContextHolder = tenantContextHolder;
        this.idpConfigRepository = idpConfigRepository;
    }

    /**
     * Decodes and validates the supplied JWT followed by signature verification
     * before returning the Claims from the JWT Payload.
     *
     * @param token the JSON Web Token
     * @return a <code>Map</code> of the JWT Claims
     * @throws JwkException if the JWT is invalid or if the JWS could not be verified
     */
    @Override
    protected Map<String, Object> decode(String token) {
        Map<String, String> headers = this.customJwtHeaderConverter.convert(token);

        // Validate "kid" header
        String keyIdHeader = headers.get(KEY_ID);
        if (keyIdHeader == null) {
            throw new InvalidTokenException("Invalid JWT/JWS: " + KEY_ID + " is a required JOSE Header");
        }
        JwkDefinitionSource.JwkDefinitionHolder jwkDefinitionHolder =
            this.jwkDefinitionSource.getDefinitionLoadIfNecessary(keyIdHeader);
        if (jwkDefinitionHolder == null) {
            throw new InvalidTokenException("Invalid JOSE Header " + KEY_ID + " (" + keyIdHeader + ")");
        }

        CustomJwkDefinition jwkDefinition = jwkDefinitionHolder.getJwkDefinition();
        // Validate "alg" header
        String algorithmHeader = headers.get(ALGORITHM);
        if (algorithmHeader == null) {
            throw new InvalidTokenException("Invalid JWT/JWS: " + ALGORITHM + " is a required JOSE Header");
        }
        if (jwkDefinition.getAlgorithm() != null && !algorithmHeader.equals(jwkDefinition.getAlgorithm().headerParamValue())) {
            throw new InvalidTokenException("Invalid JOSE Header " + ALGORITHM + " (" + algorithmHeader + ")" +
                " does not match algorithm associated to JWK with " + KEY_ID + " (" + keyIdHeader + ")");
        }

        // Verify signature
        SignatureVerifier verifier = jwkDefinitionHolder.getSignatureVerifier();
        Jwt jwt = JwtHelper.decodeAndVerify(token, verifier);

        Map<String, Object> claims = this.jsonParser.parseMap(jwt.getClaims());
        if (claims.containsKey(EXP) && claims.get(EXP) instanceof Integer) {
            Integer expiryInt = (Integer) claims.get(EXP);
            claims.put(EXP, Long.valueOf(expiryInt));
        }

        validateClaims(claims);

        return claims;
    }

    private void validateClaims(Map<String, Object> claims) {
        List<JwtClaimsSetVerifier> claimVerifiers = buildClaimVerifiers(claims);

        claimVerifiers.forEach(claimsSetVerifier -> claimsSetVerifier.verify(claims));
    }

    @SneakyThrows
    private List<JwtClaimsSetVerifier> buildClaimVerifiers(Map<String, Object> claims) {
        String tenantKey = tenantContextHolder.getTenantKey();
        Map<String, JwtClaimsSetVerifier> tenantClaimVerifiers = this.jwtClaimsSetVerifiers.getOrDefault(tenantKey, new HashMap<>());

        return buildDefaultClaimVerifiers(tenantKey, claims);
    }

    @SneakyThrows
    private List<JwtClaimsSetVerifier> buildDefaultClaimVerifiers(String tenantKey,
                                                                  Map<String, Object> claims) {
        Map<String, IdpConfigContainer> configs = idpConfigRepository.getIdpClientConfigsByTenantKey(tenantKey);

        String tokenAud = getTokenClaim(claims, "aud");
        String issuerDefinition = getIssuerDefinition(tokenAud, configs);

        if (StringUtils.isEmpty(issuerDefinition)) {
            //TODO put valid exception
            throw new Exception("Issuer not found for token with audience: " + tokenAud);
        }

        return List.of(new AudienceClaimVerifier(tokenAud), new IssuerClaimVerifier(new URL(issuerDefinition)));
    }

    private String getTokenClaim(Map<String, Object> claims, String targetClaim) throws Exception {
        String tokenClaim = (String) claims.get(targetClaim);

        if (StringUtils.isEmpty(tokenClaim)) {
            //TODO put valid exception
            throw new Exception("Claim not found: " + targetClaim);
        }
        return tokenClaim;
    }

    private List<String> getAudienceDefinitions(String tokenAud, Map<String, IdpConfigContainer> configs) {
        return configs
            .values()
            .stream()
            .map(IdpConfigContainer::getIdpPublicClientConfig)
            .map(IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig::getClientId)
            .filter(Objects::nonNull)
            .filter(clientId -> clientId.equals(tokenAud))
            .collect(Collectors.toList());
    }

    private String getIssuerDefinition(String tokenAud, Map<String, IdpConfigContainer> configs) {
        List<IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig> publicClientConfigs = configs
            .values()
            .stream()
            .map(IdpConfigContainer::getIdpPublicClientConfig)
            .collect(Collectors.toList());

        return publicClientConfigs
            .stream()
            .map(idpPublicClientConfig -> Map.entry(idpPublicClientConfig.getClientId(),
                idpPublicClientConfig.getIssuer()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            .get(tokenAud);

    }
}
