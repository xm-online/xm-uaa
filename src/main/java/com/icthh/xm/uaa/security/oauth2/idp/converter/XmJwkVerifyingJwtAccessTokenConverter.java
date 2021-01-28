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
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkDefinition;
import javassist.NotFoundException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkAttributes.ALGORITHM;
import static com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkAttributes.KEY_ID;

/**
 * This class copied from org.springframework.security.oauth2.provider.token.store.jwk.JwkVerifyingJwtAccessTokenConverter.
 * <p>
 * Reasons: we need custom implementation of JwkDefinitionSource class
 * which impossible to import and override - it has package private access.
 * <p>
 * What was changed:
 * <ul>
 * <li/>Original properties JwkDefinitionSource, JwtHeaderConverter, JwtHeaderConverter have custom implementation.
 * <li/>Property xmJwkDefinitionSource marked as non-final.
 * <li/>Added TenantContextHolder, IdpConfigRepository properties.
 * <li/>method {@link XmJwkVerifyingJwtAccessTokenConverter#validateClaims(Map)} added to build and run default validators for claims.
 * </ul>
 * <p>
 * A specialized extension of {@link JwtAccessTokenConverter} that is responsible for verifying
 * the JSON Web Signature (JWS) for a JSON Web Token (JWT) using the corresponding JSON Web Key (JWK).
 * This implementation is associated with a {@link XmJwkDefinitionSource} for looking up
 * the matching {@link XmJwkDefinition} using the value of the JWT header parameter <b>&quot;kid&quot;</b>.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XmJwkVerifyingJwtAccessTokenConverter extends JwtAccessTokenConverter {
    private XmJwkDefinitionSource xmJwkDefinitionSource;
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;
    private final XmJwtHeaderConverter xmJwtHeaderConverter = new XmJwtHeaderConverter();
    private final JsonParser jsonParser = JsonParserFactory.create();

    /**
     * Creates a new instance using the provided {@link XmJwkDefinitionSource}
     * as the primary source for looking up {@link XmJwkDefinition}(s).
     *
     * @param xmJwkDefinitionSource the source for {@link XmJwkDefinition}(s)
     * @param tenantContextHolder   tenant context holder
     * @param idpConfigRepository   tenants public config repository
     */
    public XmJwkVerifyingJwtAccessTokenConverter(XmJwkDefinitionSource xmJwkDefinitionSource,
                                                 TenantContextHolder tenantContextHolder,
                                                 IdpConfigRepository idpConfigRepository) {
        this.xmJwkDefinitionSource = xmJwkDefinitionSource;
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
        Map<String, String> headers = this.xmJwtHeaderConverter.convert(token);

        // Validate "kid" header
        String keyIdHeader = headers.get(KEY_ID);
        if (keyIdHeader == null) {
            throw new InvalidTokenException("Invalid JWT/JWS: " + KEY_ID + " is a required JOSE Header");
        }
        XmJwkDefinitionSource.XmJwkDefinitionHolder xmJwkDefinitionHolder =
            this.xmJwkDefinitionSource.getDefinitionLoadIfNecessary(keyIdHeader);
        if (xmJwkDefinitionHolder == null) {
            throw new InvalidTokenException("Invalid JOSE Header " + KEY_ID + " (" + keyIdHeader + ")");
        }

        XmJwkDefinition jwkDefinition = xmJwkDefinitionHolder.getJwkDefinition();
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
        SignatureVerifier verifier = xmJwkDefinitionHolder.getSignatureVerifier();
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

        return buildDefaultClaimVerifiers(tenantKey, claims);
    }

    @SneakyThrows
    private List<JwtClaimsSetVerifier> buildDefaultClaimVerifiers(String tenantKey, Map<String, Object> claims) {
        String tokenAud = getTokenClaim(claims, "aud");

        List<JwtClaimsSetVerifier> jwtClaimsSetVerifiers = idpConfigRepository.getJwtClaimsSetVerifiers(tenantKey, tokenAud);
        if (CollectionUtils.isEmpty(jwtClaimsSetVerifiers)) {
            throw new NotFoundException("Jwt claims verifiers for tenant: [" + tenantKey
                + "] not found. Check tenant idp configuration.");
        }

        return jwtClaimsSetVerifiers;
    }

    private String getTokenClaim(Map<String, Object> claims, String targetClaimName) {
        String tokenClaim = (String) claims.get(targetClaimName);

        if (StringUtils.isEmpty(tokenClaim)) {
            throw new InvalidTokenException("Claim not found: " + targetClaimName);
        }
        return tokenClaim;
    }
}
