package com.icthh.xm.uaa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The {@link UserAuthPasswordEncoderConfiguration} class.
 */
@Configuration
public class UserAuthPasswordEncoderConfiguration {

    @Bean("passwordEncoder")
    public PasswordEncoder passwordEncoder(ApplicationProperties applicationProperties) {
        ApplicationProperties.Security security = applicationProperties.getSecurity();
        if (security.getPasswordEncoderStrength() != null) {
            return new BCryptPasswordEncoder(security.getPasswordEncoderStrength());
        } else {
            return new BCryptPasswordEncoder();
        }
    }

}
