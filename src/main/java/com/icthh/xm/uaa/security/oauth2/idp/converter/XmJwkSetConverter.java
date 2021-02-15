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
import com.icthh.xm.uaa.security.oauth2.idp.source.XmJwkDefinitionSource;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkDefinition;
import com.icthh.xm.uaa.security.oauth2.idp.source.model.XmRsaJwkDefinition;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.uaa.security.oauth2.idp.source.model.XmJwkAttributes.*;

// FIXME: for all copied classes i suggest do the next:
//  a) Copy them without ANY modifications (except make them not final for extension) and put all of them to one package - jwk
//  b) then extend these classes with Xm specific implementations (using different packages as now). Like XmJwkSetConverter extend JwtHeaderConverter
//  - it will allow as to see clearly what we have overridden and in future we may update base class if need more easily
//  - also we can write unit tests for our extensions classes because they wil contain only custom code
/**
 *
 * This class copied from {@link org.springframework.security.oauth2.provider.token.store.jwk.JwtHeaderConverter}
 * and couldn't be imported cause of package private access.
 * Reason: we need custom implementation of JwkDefinitionSource class
 * which impossible to import and override - it has package private access.
 * <p>
 * This class is required for {@link XmJwkDefinitionSource} implementation.
 * <p>
 * What was changed: nothing was changed in this implementation
 */
public class XmJwkSetConverter implements Converter<InputStream, Set<XmJwkDefinition>> {
	private final JsonFactory factory = new JsonFactory();

	/**
	 * Converts the supplied <code>InputStream</code> to a <code>Set</code> of {@link XmJwkDefinition}(s).
	 *
	 * @param jwkSetSource the source for the JWK Set
	 * @return a <code>Set</code> of {@link XmJwkDefinition}(s)
	 * @throws JwkException if the JWK Set JSON object is invalid
	 */
	@Override
	public Set<XmJwkDefinition> convert(InputStream jwkSetSource) {
		Set<XmJwkDefinition> jwkDefinitions;

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

            jwkDefinitions = new LinkedHashSet<>();
            Map<String, String> attributes = new HashMap<>();

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
                XmJwkDefinition.PublicKeyUse publicKeyUse =
                    XmJwkDefinition.PublicKeyUse.fromValue(attributes.get(PUBLIC_KEY_USE));
                if (XmJwkDefinition.PublicKeyUse.ENC.equals(publicKeyUse)) {
                    continue;
                }

                XmJwkDefinition jwkDefinition = null;
                XmJwkDefinition.KeyType keyType =
                    XmJwkDefinition.KeyType.fromValue(attributes.get(KEY_TYPE));
                if (XmJwkDefinition.KeyType.RSA.equals(keyType)) {
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
	 * Creates a {@link XmRsaJwkDefinition} based on the supplied attributes.
	 *
	 * @param attributes the attributes used to create the {@link XmRsaJwkDefinition}
	 * @return a {@link XmJwkDefinition} representation of a RSA Key
	 * @throws JwkException if at least one attribute value is missing or invalid for a RSA Key
	 */
	private XmJwkDefinition createRsaJwkDefinition(Map<String, String> attributes) {
		// kid
		String keyId = attributes.get(KEY_ID);
		if (!StringUtils.hasText(keyId)) {
			throw new JwkException(KEY_ID + " is a required attribute for a JWK.");
		}

		// use
		XmJwkDefinition.PublicKeyUse publicKeyUse =
				XmJwkDefinition.PublicKeyUse.fromValue(attributes.get(PUBLIC_KEY_USE));
		if (!XmJwkDefinition.PublicKeyUse.SIG.equals(publicKeyUse)) {
			throw new JwkException((publicKeyUse != null ? publicKeyUse.value() : "unknown") +
					" (" + PUBLIC_KEY_USE + ") is currently not supported.");
		}

		// alg
		XmJwkDefinition.CryptoAlgorithm algorithm =
				XmJwkDefinition.CryptoAlgorithm.fromHeaderParamValue(attributes.get(ALGORITHM));
		if (algorithm != null &&
				!XmJwkDefinition.CryptoAlgorithm.RS256.equals(algorithm) &&
				!XmJwkDefinition.CryptoAlgorithm.RS384.equals(algorithm) &&
				!XmJwkDefinition.CryptoAlgorithm.RS512.equals(algorithm)) {
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

        return new XmRsaJwkDefinition(
                keyId, publicKeyUse, algorithm, modulus, exponent);
	}
}
