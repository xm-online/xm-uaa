package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.github.jhipster.config.JHipsterProperties;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@RequiredArgsConstructor
@Configuration
@EnableAuthorizationServer
@EnableWebSecurity
@Import({
    UserAuthPasswordEncoderConfiguration.class,
    UaaAccessTokenConverterConfiguration.class,
    TfaOtpConfiguration.class
})
public class UaaConfiguration extends AuthorizationServerConfigurerAdapter implements ApplicationContextAware {

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
                .antMatchers("/social/**").permitAll()
                .antMatchers("/api/register").permitAll()
                .antMatchers("/api/activate").permitAll()
                .antMatchers("/api/authenticate").permitAll()
                .antMatchers("/api/is-captcha-need").permitAll()
                .antMatchers("/api/account/reset_password/init").permitAll()
                .antMatchers("/api/account/reset_password/check").permitAll()
                .antMatchers("/api/account/reset_password/finish").permitAll()
                .antMatchers("/api/account/reset_activation_key").permitAll()
                .antMatchers("/api/profile-info").permitAll()
                .antMatchers("/api/**").authenticated()
                .antMatchers("/management/health").permitAll()
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

    private final PasswordEncoder passwordEncoder;
    private final TenantContextHolder tenantContextHolder;
    private final JHipsterProperties jHipsterProperties;
    private final DefaultAuthenticationRefreshProvider defaultAuthenticationRefreshProvider;
    private final TenantPropertiesService tenantPropertiesService;
    private final OnlineUsersService onlineUsersService;
    private final ClientDetailsService clientDetailsService;
    private final JwtTokenStore tokenStore;
    private final JwtAccessTokenConverter jwtAccessTokenConverter;
    private final OtpGenerator otpGenerator;
    private final OtpStore otpStore;
    private final OtpSendStrategy otpSendStrategy;
    private final TokenConstraintsService tokenConstraintsService;

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
            .tokenGranter(tokenGranter(endpoints));
    }

//    /**
//     * Apply the token converter (and enhancer) for token store.
//     *
//     * @return the JwtTokenStore managing the tokens.
//     */
//    @Bean
//    public JwtTokenStore tokenStore() {
//        return new JwtTokenStore(jwtAccessTokenConverter());
//    }

    private TokenGranter tokenGranter(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        List<TokenGranter> granters = new ArrayList<>(Collections.singletonList(endpoints.getTokenGranter()));
        TfaOtpTokenGranter tfaOtpTokenGranter = new TfaOtpTokenGranter(tenantContextHolder,
            tokenServices(),
            clientDetailsService,
            endpoints.getOAuth2RequestFactory(),
            tokenStore,
            authenticationManager);
        granters.add(tfaOtpTokenGranter);
        return new CompositeTokenGranter(granters);
    }
//
//    /**
//     * This bean generates an token enhancer, which manages the exchange between JWT acces tokens and Authentication
//     * in both directions.
//     *
//     * @return an access token converter configured with the authorization server's public/private keys
//     */
//    @Bean
//    public JwtAccessTokenConverter jwtAccessTokenConverter() {
//        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
//        KeyPair keyPair = new KeyStoreKeyFactory(
//            new ClassPathResource(uaaProperties.getKeyStore().getName()), uaaProperties.getKeyStore().getPassword().toCharArray())
//            .getKeyPair(uaaProperties.getKeyStore().getAlias());
//        converter.setKeyPair(keyPair);
//        return converter;
//    }


    /**
     * Apply custom token services.
     */
    @Bean
    public AuthorizationServerTokenServices tokenServices() throws UnrecoverableKeyException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, IOException {

        final DomainTokenServices tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setTokenEnhancer(jwtAccessTokenConverter);
        tokenServices.setAuthenticationRefreshProvider(defaultAuthenticationRefreshProvider);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        tokenServices.setTenantContextHolder(tenantContextHolder);
        tokenServices.setTokenConstraintsService(tokenConstraintsService);
        // OTP settings
        tokenServices.setOtpGenerator(otpGenerator);
        tokenServices.setOtpStore(otpStore);
        tokenServices.setOtpSendStrategy(otpSendStrategy);

        return tokenServices;
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
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
