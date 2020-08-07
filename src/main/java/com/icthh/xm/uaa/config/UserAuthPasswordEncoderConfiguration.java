package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.CachePasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static java.lang.Boolean.TRUE;

/**
 * The {@link UserAuthPasswordEncoderConfiguration} class.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class UserAuthPasswordEncoderConfiguration {

    @Bean("passwordEncoder")
    public PasswordEncoder passwordEncoder(ApplicationProperties applicationProperties) {
        ApplicationProperties.Security security = applicationProperties.getSecurity();
        Integer passwordEncoderStrength = security.getPasswordEncoderStrength();
        PasswordEncoder passwordEncoder = getPasswordEncoder(passwordEncoderStrength);
        if (TRUE.equals(security.getEnablePasswordCaching())) {
            passwordEncoder = new CachePasswordEncoder(passwordEncoder, security.getEnablePasswordCacheSize());
        }
        return passwordEncoder;
    }

    private PasswordEncoder getPasswordEncoder(Integer passwordEncoderStrength) {
        if (passwordEncoderStrength != null) {
            return new BCryptPasswordEncoder(passwordEncoderStrength);
        } else {
            return new BCryptPasswordEncoder();
        }
    }

}
