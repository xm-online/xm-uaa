package com.icthh.xm.uaa.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CachePasswordEncoder implements PasswordEncoder {

        private final PasswordEncoder passwordEncoder;
        private final Cache<String, CharSequence> passwordCache;

        public CachePasswordEncoder(PasswordEncoder passwordEncoder, Integer cacheSize) {
            this.passwordEncoder = passwordEncoder;
            this.passwordCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize != null ? cacheSize : 1000)
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
