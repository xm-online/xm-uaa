package com.icthh.xm.uaa.service.util;

import java.security.SecureRandom;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utility class for generating random Strings.
 */
public final class RandomUtil {

    private static final int DEF_COUNT = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomUtil() {
    }

    /**
     * Generate a password.
     *
     * @return the generated password
     */
    public static String generatePassword() {
        return RandomStringUtils.random(DEF_COUNT, 0, 0, true, true, (char[])null, RANDOM);
    }

    /**
     * Generate an activation key.
     *
     * @return the generated activation key
     */
    public static String generateActivationKey() {
        return RandomStringUtils.random(DEF_COUNT, 0, 0, false, true, (char[])null, RANDOM);
    }

    /**
    * Generate a reset key.
    *
    * @return the generated reset key
    */
    public static String generateResetKey() {
        return RandomStringUtils.random(DEF_COUNT, 0, 0, false, true, (char[])null, RANDOM);
    }
}
