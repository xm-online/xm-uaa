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
