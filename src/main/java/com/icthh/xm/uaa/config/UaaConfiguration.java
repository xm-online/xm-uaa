package com.icthh.xm.uaa.config;

import com.icthh.xm.uaa.security.AuthenticationRefreshProvider;
import com.icthh.xm.uaa.security.AuthenticationRefreshProviderResolver;
import com.icthh.xm.uaa.security.AuthoritiesConstants;
import com.icthh.xm.uaa.security.DomainJwtAccessTokenConverter;
import com.icthh.xm.uaa.security.DomainTokenServices;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import io.github.jhipster.config.JHipsterProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CorsFilter;

@RequiredArgsConstructor
@Configuration
@EnableAuthorizationServer
public class UaaConfiguration extends AuthorizationServerConfigurerAdapter {

    @RequiredArgsConstructor
    @EnableResourceServer
    public static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private final TokenStore tokenStore;
        private final JHipsterProperties jHipsterProperties;
        private final CorsFilter corsFilter;


        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
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
                .antMatchers("/api/account/reset_password/finish").permitAll()
                .antMatchers("/api/profile-info").permitAll()
                .antMatchers("/api/**").authenticated()
                .antMatchers("/management/health").permitAll()
                .antMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .antMatchers("/v2/api-docs/**").permitAll()
                .antMatchers("/swagger-resources/configuration/ui").permitAll()
                .antMatchers("/swagger-ui/index.html").hasAuthority(AuthoritiesConstants.ADMIN);
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("jhipster-uaa").tokenStore(tokenStore);
        }
    }

    private final ApplicationProperties applicationProperties;
    private final JHipsterProperties jHipsterProperties;
    private final AuthenticationRefreshProviderResolver authenticationRefreshProviderResolver;
    private final TenantPropertiesService tenantPropertiesService;

    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate keyUriRestTemplate;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        /*
        For a better client design, this should be done by a ClientDetailsService (similar to UserDetailsService).
         */
        clients.inMemory()
            .withClient("web_app")
            .scopes("openid")
            .autoApprove(true)
            .authorizedGrantTypes("implicit","refresh_token", "password", "authorization_code")
            .and()
            .withClient(jHipsterProperties.getSecurity().getClientAuthorization().getClientId())
            .secret(jHipsterProperties.getSecurity().getClientAuthorization().getClientSecret())
            .scopes("web-app")
            .autoApprove(true)
            .authorizedGrantTypes("client_credentials");
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
            .authenticationManager(authenticationManager)
            .accessTokenConverter(jwtAccessTokenConverter())
            .tokenServices(tokenServices());
    }

    @Autowired
    @Qualifier("authenticationManagerBean")
    private AuthenticationManager authenticationManager;

    /**
     * Apply the token converter (and enhander) for token store.
     */
    @Bean
    public JwtTokenStore tokenStore() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
        KeyStoreException, IOException {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    /**
     * Apply custom token services.
     */
    @Bean
    public AuthorizationServerTokenServices tokenServices()
        throws UnrecoverableKeyException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, IOException {
        final DomainTokenServices tokenServices = new DomainTokenServices();
        tokenServices.setTokenStore(tokenStore());
        tokenServices.setTokenEnhancer(jwtAccessTokenConverter());
        tokenServices.setAuthenticationRefreshProvider(authenticationRefreshProviderResolver);
        tokenServices.setApplicationProperties(applicationProperties);
        tokenServices.setTenantPropertiesService(tenantPropertiesService);
        tokenServices.setSupportRefreshToken(true);

        return tokenServices;
    }

    /**
     * This bean generates an token enhancer, which manages the exchange between JWT acces tokens and Authentication
     * in both directions.
     *
     * @return an access token converter configured with the authorization server's public/private keys
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter()
        throws IOException, KeyStoreException, CertificateException,
        NoSuchAlgorithmException, UnrecoverableKeyException {
        DomainJwtAccessTokenConverter converter = new DomainJwtAccessTokenConverter();

        InputStream stream = new ClassPathResource(Constants.KEYSTORE_FILE).getInputStream();
        KeyStore store = KeyStore.getInstance(Constants.KEYSTOPE_TYPE);

        store.load(stream, "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)store.getKey("selfsigned", "password".toCharArray());

        PublicKey publicKey = getKeyFromConfigServer(keyUriRestTemplate);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        converter.setKeyPair(keyPair);
        return converter;
    }

    private PublicKey getKeyFromConfigServer(RestTemplate keyUriRestTemplate) throws CertificateException {
        HttpEntity<Void> request = new HttpEntity<>(new HttpHeaders());
        String content = keyUriRestTemplate
            .exchange("http://config/api/token_key", HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw  new CertificateException("Received empty public key from config.");
        }

        InputStream fin = new ByteArrayInputStream(content.getBytes());

        CertificateFactory f = CertificateFactory.getInstance(Constants.CERTIFICATE);
        X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);
        return certificate.getPublicKey();
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
    }

    @Bean
    public AuthenticationRefreshProvider defaultAuthenticationRefreshProvider() {
        return null;
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
