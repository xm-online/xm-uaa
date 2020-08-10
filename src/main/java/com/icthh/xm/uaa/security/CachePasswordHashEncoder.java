package com.icthh.xm.uaa.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CachePasswordHashEncoder implements PasswordEncoder {

    private static final int DEFAULT_CACHE_SIZE = 1000;

    private final PasswordEncoder passwordEncoder;
        private final Cache<String, CharSequence> passwordCache;

        public CachePasswordHashEncoder(PasswordEncoder passwordEncoder, Integer cacheSize) {
            this.passwordEncoder = passwordEncoder;
            this.passwordCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize != null ? cacheSize : DEFAULT_CACHE_SIZE)
                .build();
        }

        @Override
        public String encode(CharSequence rawPassword) {
            return passwordEncoder.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            CharSequence cachedPassword = passwordCache.getIfPresent(encodedPassword);
            if (cachedPassword != null && cachedPassword.equals(rawPassword)) {
                return true;
            } else {
                boolean matchResult = passwordEncoder.matches(rawPassword, encodedPassword);
                if (matchResult) {
                    passwordCache.put(encodedPassword, rawPassword);
                }
                return matchResult;
            }
        }

        @Override
        public boolean upgradeEncoding(String encodedPassword) {
            return passwordEncoder.upgradeEncoding(encodedPassword);
        }
    }
