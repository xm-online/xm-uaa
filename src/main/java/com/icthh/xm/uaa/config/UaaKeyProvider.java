package com.icthh.xm.uaa.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

/**
 * Single source for the private key that signs platform JWTs: {@code application.private-key} if set, otherwise
 * the configured keystore. Also tells whether the service still signs with the keystore committed to the public
 * repository ({@code src/main/resources/keystore.p12}), whose private key is public knowledge.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UaaKeyProvider {

    /** The keystore committed to the public repository; the reference for the "default keystore in use" check. */
    static final String DEFAULT_COMMITTED_KEYSTORE = "keystore.p12";

    private final ApplicationProperties applicationProperties;

    public PrivateKey getPrivateKey() throws IOException, KeyStoreException, CertificateException,
        NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeySpecException {
        final String privateKey = applicationProperties.getPrivateKey();
        if (!StringUtils.isEmpty(privateKey)) {
            log.info("Key was loaded from memory by application property: application.private-key");

            return initPrivateKey(Base64.decode(applicationProperties.getPrivateKey().getBytes(StandardCharsets.UTF_8)));
        } else {
            log.info("Keystore location {}", applicationProperties.getKeystoreFile());
            try (InputStream stream = openKeystore(applicationProperties.getKeystoreFile())) {
                return initPrivateKeyFromKeystore(stream);
            }
        }
    }

    /**
     * Whether JWTs are still signed with the keystore committed to the public repository. True only when no
     * {@code application.private-key} is configured and the configured keystore is byte-for-byte the committed one.
     * No keystore is opened - a raw byte comparison is enough to tell the committed default was not replaced.
     */
    public boolean isDefaultKeystoreInUse() {
        if (!StringUtils.isEmpty(applicationProperties.getPrivateKey())) {
            return false; // own key provided, the committed keystore is not used for signing
        }
        try (InputStream inUse = openKeystore(applicationProperties.getKeystoreFile());
             InputStream committed = new ClassPathResource(DEFAULT_COMMITTED_KEYSTORE).getInputStream()) {
            return Arrays.equals(FileCopyUtils.copyToByteArray(inUse), FileCopyUtils.copyToByteArray(committed));
        } catch (IOException e) {
            // Cannot read one of the keystores - do not claim a misconfiguration we could not confirm.
            log.error("Could not compare the configured keystore with the committed default keystore", e);
            return false;
        }
    }

    private InputStream openKeystore(String keystoreFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(keystoreFile);
        return resource.exists() ? resource.getInputStream() : new FileInputStream(keystoreFile);
    }

    private PrivateKey initPrivateKeyFromKeystore(InputStream stream) throws KeyStoreException, UnrecoverableKeyException,
        NoSuchAlgorithmException, IOException, CertificateException {
        KeyStore store = KeyStore.getInstance(Constants.KEYSTORE_TYPE);
        store.load(stream, applicationProperties.getKeystorePassword().toCharArray());
        return (PrivateKey) store.getKey(applicationProperties.getKeystoreKeyName(),
            applicationProperties.getKeystorePassword().toCharArray());
    }

    private PrivateKey initPrivateKey(final byte[] encodedPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        return factory.generatePrivate(keySpec);
    }
}
