package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.UaaAuthenticationProvider;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationEmbedded;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationOtpMs;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationProvider;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import({
    UserAuthPasswordEncoderConfiguration.class,
    TfaOtpConfiguration.class
})
public class UaaWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final AuthenticationProvider uaaAuthenticationProvider;
    private final TfaOtpAuthenticationEmbedded tfaOtpAuthenticationEmbedded;
    private final TfaOtpAuthenticationOtpMs tfaOtpAuthenticationOtpMs;

    public UaaWebSecurityConfiguration(PasswordEncoder passwordEncoder,
                                       UserDetailsService userDetailsService,
                                       AuthenticationManagerBuilder authenticationManagerBuilder,
                                       @Qualifier("uaaAuthenticationProvider") UaaAuthenticationProvider uaaAuthenticationProvider,
                                       TfaOtpAuthenticationEmbedded tfaOtpAuthenticationEmbedded,
                                       TfaOtpAuthenticationOtpMs tfaOtpAuthenticationOtpMs) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.uaaAuthenticationProvider = uaaAuthenticationProvider;
        this.tfaOtpAuthenticationEmbedded = tfaOtpAuthenticationEmbedded;
        this.tfaOtpAuthenticationOtpMs = tfaOtpAuthenticationOtpMs;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(uaaAuthenticationProvider);
        auth.authenticationProvider(tfaOtpAuthenticationEmbedded);
        auth.authenticationProvider(tfaOtpAuthenticationOtpMs);
    }

    @PostConstruct
    public void init() throws Exception {
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .antMatchers("/app/**/*.{js,html}")
            .antMatchers("/bower_components/**")
            .antMatchers("/i18n/**")
            .antMatchers("/content/**")
            .antMatchers("/swagger-ui/index.html")
            .antMatchers("/test/**")
            .antMatchers("/h2-console/**");
    }

    @Bean
    public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
        return new SecurityEvaluationContextExtension();
    }
}
