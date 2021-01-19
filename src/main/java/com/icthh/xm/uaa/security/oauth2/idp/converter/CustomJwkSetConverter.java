/*
 * Copyright 2012-2019 the original author or authors.
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomRsaJwkDefinition;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.uaa.security.oauth2.idp.source.model.CustomJwkAttributes.*;


/**
 * A {@link Converter} that converts the supplied <code>InputStream</code> to a <code>Set</code> of {@link CustomJwkDefinition}(s).
 * The source of the <code>InputStream</code> <b>must be</b> a JWK Set representation which is a JSON object
 * that has a &quot;keys&quot; member and its value is an array of JWKs.
 * <br>
 * <br>
 *
 * <b>NOTE:</b> The Key Type (&quot;kty&quot;) currently supported by this {@link Converter} is {@link CustomJwkDefinition.KeyType#RSA}.
 * <br>
 * <br>
 *
 * @see CustomJwkDefinition
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7517#page-10">JWK Set Format</a>
 *
 * @author Joe Grandja
 * @author Vedran Pavic
 * @author Michael Duergner
 */
public class CustomJwkSetConverter implements Converter<InputStream, Set<CustomJwkDefinition>> {
	private final JsonFactory factory = new JsonFactory();

	/**
	 * Converts the supplied <code>InputStream</code> to a <code>Set</code> of {@link CustomJwkDefinition}(s).
	 *
	 * @param jwkSetSource the source for the JWK Set
	 * @return a <code>Set</code> of {@link CustomJwkDefinition}(s)
	 * @throws JwkException if the JWK Set JSON object is invalid
	 */
	@Override
	public Set<CustomJwkDefinition> convert(InputStream jwkSetSource) {
		Set<CustomJwkDefinition> jwkDefinitions;

        try (JsonParser parser = this.factory.createParser(jwkSetSource)) {

            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new JwkException("Invalid JWK Set Object.");
            }
            if (parser.nextToken() != JsonToken.FIELD_NAME) {
                throw new JwkException("Invalid JWK Set Object.");
            }
            if (!parser.getCurrentName().equals(KEYS)) {
                throw new JwkException("Invalid JWK Set Object. The JWK Set MUST have a " + KEYS + " attribute.");
            }
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new JwkException("Invalid JWK Set Object. The JWK Set MUST have an array of JWK(s).");
            }

            jwkDefinitions = new LinkedHashSet<CustomJwkDefinition>();
            Map<String, String> attributes = new HashMap<String, String>();

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                attributes.clear();
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    String attributeName = parser.getCurrentName();
                    // gh-1082 - skip arrays such as x5c as we can't deal with them yet
                    if (parser.nextToken() == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                        }
                    } else {
                        attributes.put(attributeName, parser.getValueAsString());
                    }
                }

                // gh-1470 - skip unsupported public key use (enc) without discarding the entire set
                CustomJwkDefinition.PublicKeyUse publicKeyUse =
                    CustomJwkDefinition.PublicKeyUse.fromValue(attributes.get(PUBLIC_KEY_USE));
                if (CustomJwkDefinition.PublicKeyUse.ENC.equals(publicKeyUse)) {
                    continue;
                }

                CustomJwkDefinition jwkDefinition = null;
                CustomJwkDefinition.KeyType keyType =
                    CustomJwkDefinition.KeyType.fromValue(attributes.get(KEY_TYPE));
                if (CustomJwkDefinition.KeyType.RSA.equals(keyType)) {
                    jwkDefinition = this.createRsaJwkDefinition(attributes);
                }
                if (jwkDefinition != null) {
                    if (!jwkDefinitions.add(jwkDefinition)) {
                        throw new JwkException("Duplicate JWK found in Set: " +
                            jwkDefinition.getKeyId() + " (" + KEY_ID + ")");
                    }
                }
            }

        } catch (IOException ex) {
            throw new JwkException("An I/O error occurred while reading the JWK Set: " + ex.getMessage(), ex);
        }

		return jwkDefinitions;
	}

	/**
	 * Creates a {@link CustomRsaJwkDefinition} based on the supplied attributes.
	 *
	 * @param attributes the attributes used to create the {@link CustomRsaJwkDefinition}
	 * @return a {@link CustomJwkDefinition} representation of a RSA Key
	 * @throws JwkException if at least one attribute value is missing or invalid for a RSA Key
	 */
	private CustomJwkDefinition createRsaJwkDefinition(Map<String, String> attributes) {
		// kid
		String keyId = attributes.get(KEY_ID);
		if (!StringUtils.hasText(keyId)) {
			throw new JwkException(KEY_ID + " is a required attribute for a JWK.");
		}

		// use
		CustomJwkDefinition.PublicKeyUse publicKeyUse =
				CustomJwkDefinition.PublicKeyUse.fromValue(attributes.get(PUBLIC_KEY_USE));
		if (!CustomJwkDefinition.PublicKeyUse.SIG.equals(publicKeyUse)) {
			throw new JwkException((publicKeyUse != null ? publicKeyUse.value() : "unknown") +
					" (" + PUBLIC_KEY_USE + ") is currently not supported.");
		}

		// alg
		CustomJwkDefinition.CryptoAlgorithm algorithm =
				CustomJwkDefinition.CryptoAlgorithm.fromHeaderParamValue(attributes.get(ALGORITHM));
		if (algorithm != null &&
				!CustomJwkDefinition.CryptoAlgorithm.RS256.equals(algorithm) &&
				!CustomJwkDefinition.CryptoAlgorithm.RS384.equals(algorithm) &&
				!CustomJwkDefinition.CryptoAlgorithm.RS512.equals(algorithm)) {
			throw new JwkException(algorithm.standardName() + " (" + ALGORITHM + ") is currently not supported.");
		}

		// n
		String modulus = attributes.get(RSA_PUBLIC_KEY_MODULUS);
		if (!StringUtils.hasText(modulus)) {
			throw new JwkException(RSA_PUBLIC_KEY_MODULUS + " is a required attribute for a RSA JWK.");
		}

		// e
		String exponent = attributes.get(RSA_PUBLIC_KEY_EXPONENT);
		if (!StringUtils.hasText(exponent)) {
			throw new JwkException(RSA_PUBLIC_KEY_EXPONENT + " is a required attribute for a RSA JWK.");
		}

        return new CustomRsaJwkDefinition(
                keyId, publicKeyUse, algorithm, modulus, exponent);
	}
}
