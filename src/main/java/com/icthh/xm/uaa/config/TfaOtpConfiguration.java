package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.security.oauth2.otp.DefaultOtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.DomainUserDetailsOtpStore;
import com.icthh.xm.uaa.security.oauth2.otp.EmailOtpSender;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.security.oauth2.otp.PseudoTimeOtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.UserLoginOtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationProvider;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.mail.MailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

/**
 * The {@link TfaOtpConfiguration} class.
 */
@Configuration
@Import({
    TenantContextConfiguration.class,
    XmRequestContextConfiguration.class
})
public class TfaOtpConfiguration {

    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
    private final TenantPropertiesService tenantPropertiesService;
    private final MailService mailService;
    private final UserService userService;

    public TfaOtpConfiguration(TenantContextHolder tenantContextHolder,
                               XmRequestContextHolder xmRequestContextHolder,
                               TenantPropertiesService tenantPropertiesService,
                               MailService mailService,
                               UserService userService) {
        this.tenantContextHolder = tenantContextHolder;
        this.xmRequestContextHolder = xmRequestContextHolder;
        this.tenantPropertiesService = tenantPropertiesService;
        this.mailService = mailService;
        this.userService = userService;
    }

    @Bean
    OtpGenerator otpGenerator() {
        return new PseudoTimeOtpGenerator();
    }

    @Bean("otpEncoder")
    PasswordEncoder otpEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    OtpStore otpStore() {
        return new DomainUserDetailsOtpStore(otpEncoder());
    }

    @Bean
    OtpSendStrategy otpSendStrategy() {
        return new UserLoginOtpSendStrategy(tenantPropertiesService, otpSenderFactory());
    }

    @Bean
    OtpSenderFactory otpSenderFactory() {
        return new DefaultOtpSenderFactory(emailOtpSender());
    }

    @Bean
    EmailOtpSender emailOtpSender() {
        return new EmailOtpSender(tenantContextHolder, xmRequestContextHolder, mailService, userService);
    }

    @Bean
    TfaOtpAuthenticationProvider tfaOtpAuthenticationProvider(UserDetailsService userDetailsService) {
        return new TfaOtpAuthenticationProvider(userDetailsService, otpEncoder());
    }

}
