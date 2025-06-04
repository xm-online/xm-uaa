package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.uaa.security.oauth2.otp.SmsOtpSender;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationEmbedded;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationOtpMs;
import com.icthh.xm.uaa.service.otp.OtpService;
import com.icthh.xm.uaa.security.oauth2.otp.DefaultOtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.DomainUserDetailsOtpStore;
import com.icthh.xm.uaa.security.oauth2.otp.EmailOtpSender;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSenderFactory;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.security.oauth2.otp.PseudoTimeOtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.UserLoginOtpSendStrategy;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The {@link TfaOtpConfiguration} class.
 */
@Configuration
@Import({
    TenantContextConfiguration.class,
    XmRequestContextConfiguration.class
})
public class TfaOtpConfiguration {

    private final TenantPropertiesService tenantPropertiesService;
    private final SmsOtpSender smsOtpSender;
    private final EmailOtpSender emailOtpSender;

    public TfaOtpConfiguration(TenantPropertiesService tenantPropertiesService,
                               SmsOtpSender smsOtpSender, EmailOtpSender emailOtpSender) {
        this.tenantPropertiesService = tenantPropertiesService;
        this.smsOtpSender = smsOtpSender;
        this.emailOtpSender = emailOtpSender;
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
        return new DefaultOtpSenderFactory(emailOtpSender, smsOtpSender);
    }

    @Bean
    TfaOtpAuthenticationEmbedded tfaOtpAuthenticationEmbedded(UserDetailsService userDetailsService) {
        return new TfaOtpAuthenticationEmbedded(userDetailsService, otpEncoder());
    }

    @Bean
    TfaOtpAuthenticationOtpMs tfaOtpAuthenticationOtpMs(UserDetailsService userDetailsService, OtpService otpService) {
        return new TfaOtpAuthenticationOtpMs(userDetailsService, otpEncoder(), otpService);
    }

}
