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
package com.icthh.xm.uaa.security.oauth2.idp.source.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This class copied from {@link org.springframework.security.oauth2.provider.token.store.jwk.RsaJwkDefinition}.
 * and couldn't be imported cause of package private access.
 * Reason: we need custom implementation of JwkDefinitionSource class
 * which impossible to import and override - it has package private access.
 * <p>
 * This class is org.springframework.security.oauth2.provider.token.store.jwk.RsaJwkDefinition implementation.
 * <p>
 * What was changed: nothing was changed in this implementation
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class XmRsaJwkDefinition extends XmJwkDefinition {
	private final String modulus;
	private final String exponent;

	/**
	 * Creates an instance of a RSA JSON Web Key (JWK).
	 *
	 * @param keyId the Key ID
	 * @param publicKeyUse the intended use of the Public Key
	 * @param algorithm the algorithm intended to be used
	 * @param modulus the modulus value for the Public Key
	 * @param exponent the exponent value for the Public Key
	 */
    public XmRsaJwkDefinition(String keyId,
                              PublicKeyUse publicKeyUse,
                              CryptoAlgorithm algorithm,
                              String modulus,
                              String exponent) {

		super(keyId, KeyType.RSA, publicKeyUse, algorithm);
		this.modulus = modulus;
		this.exponent = exponent;
	}

	/**
	 * @return the modulus value for the Public Key
	 */
    public String getModulus() {
		return this.modulus;
	}

	/**
	 * @return the exponent value for the Public Key
	 */
    public String getExponent() {
		return this.exponent;
	}
}
