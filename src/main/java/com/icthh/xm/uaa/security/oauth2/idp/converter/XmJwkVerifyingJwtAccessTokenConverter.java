package com.icthh.xm.uaa.security.oauth2.idp.converter;

import com.icthh.xm.uaa.security.oauth2.idp.config.IdpConfigRepository;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.jwk.JwkVerifyingJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;

import java.util.List;
import java.util.Map;

/**
 * This class responsible for managing basic token signature and claim verification
 */
@EqualsAndHashCode(callSuper = true)
// FIXME: seems we need to cover the class by Unit test as we have changed it.
public class XmJwkVerifyingJwtAccessTokenConverter extends JwkVerifyingJwtAccessTokenConverter {
    private final IdpConfigRepository idpConfigRepository;

    /**
     * Creates a new instance using the provided {@link XmJwkDefinitionSource}
     * as the primary source for looking up {@link JwkDefinition}(s).
     *
     * @param xmJwkDefinitionSource the source for {@link XmJwkDefinitionSource}(s)
     * @param idpConfigRepository   tenants public config repository
     */
    public XmJwkVerifyingJwtAccessTokenConverter(XmJwkDefinitionSource xmJwkDefinitionSource,
                                                 IdpConfigRepository idpConfigRepository) {
        super(xmJwkDefinitionSource);
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
        Map<String, Object> claims = super.decode(token);

        validateClaims(claims);

        return claims;
    }

    private void validateClaims(Map<String, Object> claims) {
        List<JwtClaimsSetVerifier> claimVerifiers = getDefaultClaimVerifiers(claims);

        claimVerifiers.forEach(claimsSetVerifier -> claimsSetVerifier.verify(claims));
    }

    @SneakyThrows
    private List<JwtClaimsSetVerifier> getDefaultClaimVerifiers(Map<String, Object> claims) {
        String tokenAud = getTokenClaim(claims, "aud");

        return idpConfigRepository.getJwtClaimsSetVerifiers(tokenAud);
    }

    private String getTokenClaim(Map<String, Object> claims, String targetClaimName) {
        String tokenClaim = (String) claims.get(targetClaimName);

        if (StringUtils.isEmpty(tokenClaim)) {
            throw new InvalidTokenException("Claim not found: " + targetClaimName);
        }
        return tokenClaim;
    }
}
