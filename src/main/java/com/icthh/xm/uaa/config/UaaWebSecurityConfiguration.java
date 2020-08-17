package com.icthh.xm.uaa.config;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.uaa.security.UaaAuthenticationProvider;
import com.icthh.xm.uaa.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.icthh.xm.uaa.security.oauth2.OAuth2StatelessAuthenticationSuccessHandler;
import com.icthh.xm.uaa.security.oauth2.XmOAuth2UserService;
import com.icthh.xm.uaa.security.oauth2.XmOidcUserService;
import com.icthh.xm.uaa.security.oauth2.tfa.TfaOtpAuthenticationProvider;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import({
    UserAuthPasswordEncoderConfiguration.class,
    TfaOtpConfiguration.class,
    OidcConfiguration.class
})
public class UaaWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final CorsFilter corsFilter;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final AuthenticationProvider uaaAuthenticationProvider;
    private final TfaOtpAuthenticationProvider tfaOtpAuthenticationProvider;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;
    private final OAuth2StatelessAuthenticationSuccessHandler authenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler authenticationFailureHandler;
//    private final XmAuthenticationManagerResolver authenticationManagerResolver;
    private final XmOidcUserService xmOidcUserService;
    private final XmOAuth2UserService xmOAuth2UserService;

    public UaaWebSecurityConfiguration(PasswordEncoder passwordEncoder,
                                       UserDetailsService userDetailsService,
                                       CorsFilter corsFilter,
                                       AuthenticationManagerBuilder authenticationManagerBuilder,
                                       @Qualifier("uaaAuthenticationProvider") UaaAuthenticationProvider uaaAuthenticationProvider,
                                       TfaOtpAuthenticationProvider tfaOtpAuthenticationProvider,
                                       AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
                                       OAuth2StatelessAuthenticationSuccessHandler authenticationSuccessHandler,
                                       OAuth2AuthenticationFailureHandler authenticationFailureHandler,
//                                       XmAuthenticationManagerResolver authenticationManagerResolver,
                                       XmOidcUserService xmOidcUserService,
                                       XmOAuth2UserService xmOAuth2UserService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.corsFilter = corsFilter;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.uaaAuthenticationProvider = uaaAuthenticationProvider;
        this.tfaOtpAuthenticationProvider = tfaOtpAuthenticationProvider;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
//        this.authenticationManagerResolver = authenticationManagerResolver;
        this.xmOidcUserService = xmOidcUserService;
        this.xmOAuth2UserService = xmOAuth2UserService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
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
            .antMatchers("/api/users/accept-terms-of-conditions/*").permitAll()
            .antMatchers("/api/**").authenticated()
            .antMatchers("/management/health").permitAll()
            .antMatchers("/management/prometheus").permitAll()
            .antMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
            .antMatchers("/v2/api-docs/**").permitAll()
            .antMatchers("/swagger-resources/configuration/ui").permitAll()
            .antMatchers("/swagger-ui/index.html").hasAuthority(RoleConstant.SUPER_ADMIN)
            .and()
            .oauth2Login()
                .authorizationEndpoint()
                    .authorizationRequestRepository(authorizationRequestRepository)
                .and()
                .userInfoEndpoint()
                    .oidcUserService(xmOidcUserService)
                    .userService(xmOAuth2UserService)
                    .and()
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .and()
            .oauth2Client()
            .and()
            .oauth2ResourceServer()
//            .authenticationManagerResolver(authenticationManagerResolver)
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter());
    }

    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(uaaAuthenticationProvider);
        auth.authenticationProvider(tfaOtpAuthenticationProvider);
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
