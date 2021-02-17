package com.icthh.xm.uaa.security.oauth2.idp.validation.verifiers;

import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.store.JwtClaimsSetVerifier;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * A {@link JwtClaimsSetVerifier} that verifies the Audience (aud) claim contained in the
 * JWT Claims Set against the <code>audience</code> supplied to the constructor.
 *
 * @see JwtClaimsSetVerifier
 */
public class AudienceClaimVerifier implements JwtClaimsSetVerifier {
	private static final String AUD_CLAIM = "aud";
	private final String audience;

	public AudienceClaimVerifier(String audience) {
		Assert.notNull(audience, "audience cannot be null");
		this.audience = audience;
	}

	@Override
	public void verify(Map<String, Object> claims) throws InvalidTokenException {
		if (!CollectionUtils.isEmpty(claims) && claims.containsKey(AUD_CLAIM)) {
			String jwtAudience = (String)claims.get(AUD_CLAIM);
			if (!jwtAudience.equals(this.audience)) {
				throw new InvalidTokenException("Invalid Audience (aud) claim: " + jwtAudience);
			}
		}
	}
}
