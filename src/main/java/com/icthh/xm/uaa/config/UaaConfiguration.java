package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.security.DomainTokenServices;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.security.UserSecurityValidator;
import com.icthh.xm.uaa.security.oauth2.athorization.code.CustomAuthorizationCodeServices;
import com.icthh.xm.uaa.security.oauth2.idp.XmJwkTokenStore;
import com.icthh.xm.uaa.security.oauth2.idp.IdpTokenGranter;
import com.icthh.xm.uaa.security.oauth2.otp.OtpGenerator;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSendStrategy;
import com.icthh.xm.uaa.security.oauth2.otp.OtpStore;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpTokenGranter;
import com.icthh.xm.uaa.security.provider.DefaultAuthenticationRefreshProvider;
import com.icthh.xm.uaa.service.IdpIdTokenMappingService;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.service.UserLoginService;
import com.icthh.xm.uaa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.CompositeTokenGranter;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableAuthorizationServer
@Import({
    TfaOtpConfiguration.class,
    UserAuthPasswordEncoderConfiguration.class,
    UaaAccessTokenConverterConfiguration.class
})
public class UaaConfiguration extends AuthorizationServerConfigurerAdapter {

    @RequiredArgsConstructor
    @EnableResourceServer
    public static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private final TokenStore tokenStore;
        private final CorsFilter corsFilter;

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException)
                    -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .and()
                .csrf()
                .disable()
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .headers()
                .frameOptions()
                .disable()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/register").permitAll()
                .antMatchers("/api/activate").permitAll()
                .antMatchers("/api/authenticate").permitAll()
                .antMatchers("/api/is-captcha-need").permitAll()
                .antMatchers("/api/account/reset_password/init").permitAll()
                .antMatchers("/api/account/reset_password/check").permitAll()
                .antMatchers("/api/account/reset_password/finish").permitAll()
                .antMatchers("/api/account/reset_activation_key").permitAll()
                .antMatchers("/api/profile-info").permitAll()
                .antMatchers("/api/users/accept-terms-of-conditions/*").permitAll()
                .antMatchers("/api/**").authenticated()
                .antMatchers("/management/health").permitAll()
                .antMatchers("/management/prometheus").permitAll()
                .antMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
                .antMatchers("/v2/api-docs/**").permitAll()
                .antMatchers("/swagger-resources/configuration/ui").permitAll()
                .antMatchers("/swagger-ui/index.html").hasAuthority(RoleConstant.SUPER_ADMIN);
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("jhipster-uaa").tokenStore(tokenStore);
        }
    }

    @Autowired
    @Qualifier("authenticationManagerBean")
    private AuthenticationManager authenticationManager;

    private final OtpStore otpStore;
    private final UserService userService;
    private final UserLoginService userLoginService;
    private final JwtTokenStore tokenStore;
    private final OtpGenerator otpGenerator;
    private final PasswordEncoder passwordEncoder;
    private final OtpSendStrategy otpSendStrategy;
    private final ClientDetailsService clientDetailsService;
    private final TenantContextHolder tenantContextHolder;
    private final TenantPropertiesService tenantPropertiesService;
    private final JwtAccessTokenConverter jwtAccessTokenConverter;
    private final TokenConstraintsService tokenConstraintsService;
    private final IdpIdTokenMappingService idpIdTokenMappingService;
    private final DomainUserDetailsService domainUserDetailsService;
    private final CustomAuthorizationCodeServices customAuthorizationCodeServices;
    private final DefaultAuthenticationRefreshProvider defaultAuthenticationRefreshProvider;
    private final UserSecurityValidator userSecurityValidator;
    private final XmJwkTokenStore jwkTokenStore;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientDetailsService);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
            .authenticationManager(authenticationManager)
            .accessTokenConverter(jwtAccessTokenConverter)
            .tokenServices(tokenServices())
            .tokenGranter(tokenGranter(endpoints))
            .authorizationCodeServices(customAuthorizationCodeServices)
        ;
    }

    private TokenGranter tokenGranter(AuthorizationServerEndpointsConfigurer endpoints) {
        List<TokenGranter> granters = new ArrayList<>(Collections.singletonList(endpoints.getTokenGranter()));
        TfaOtpTokenGranter tfaOtpTokenGranter = new TfaOtpTokenGranter(tenantContextHolder,
            tokenServices(),
            clientDetailsService,
            endpoints.getOAuth2RequestFactory(),
            tokenStore,
            authenticationManager);

        IdpTokenGranter idpTokenGranter = new IdpTokenGranter(
            tokenServices(),
            clientDetailsService,
            endpoints.getOAuth2RequestFactory(),
            jwkTokenStore,
            domainUserDetailsService,
            userService,
            userLoginService,
            idpIdTokenMappingService,
            tenantContextHolder);

        granters.add(tfaOtpTokenGranter);
        granters.add(idpTokenGranter);

        return new CompositeTokenGranter(granters);
    }

    /**
     * Apply custom token services.
     */
    @Bean
    public AuthorizationServerTokenServices tokenServices() {

        final DomainTokenServices tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setJwkTokenStore(jwkTokenStore);
        tokenServices.setTokenEnhancer(jwtAccessTokenConverter);
        tokenServices.setAuthenticationRefreshProvider(defaultAuthenticationRefreshProvider);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        tokenServices.setTenantContextHolder(tenantContextHolder);
        tokenServices.setTokenConstraintsService(tokenConstraintsService);
        tokenServices.setUserService(userService);
        tokenServices.setUserSecurityValidator(userSecurityValidator);
        // OTP settings
        tokenServices.setOtpGenerator(otpGenerator);
        tokenServices.setOtpStore(otpStore);
        tokenServices.setOtpSendStrategy(otpSendStrategy);

        return tokenServices;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
        oauthServer.passwordEncoder(passwordEncoder);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
