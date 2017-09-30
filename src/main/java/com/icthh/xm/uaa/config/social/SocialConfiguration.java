package com.icthh.xm.uaa.config.social;

import com.icthh.xm.uaa.repository.CustomSocialUsersConnectionRepository;
import com.icthh.xm.uaa.repository.SocialConfigRepository;
import com.icthh.xm.uaa.repository.SocialUserConnectionRepository;
import com.icthh.xm.uaa.security.social.CustomSignInAdapter;
import com.icthh.xm.uaa.social.connect.web.ConnectSupport;
import com.icthh.xm.uaa.social.connect.web.DomainConnectionFactoryLocator;
import com.icthh.xm.uaa.social.connect.web.HttpSessionSessionStrategy;
import com.icthh.xm.uaa.social.connect.web.ProviderSignInUtils;
import com.icthh.xm.uaa.social.connect.web.SessionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.social.UserIdSource;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.security.AuthenticationNameUserIdSource;

/**
 * Basic Spring Social configuration.
 *
 * <p>
 * Creates the beans necessary to manage Connections to social services and
 * link accounts from those services to internal Users.
 */
@Configuration
public class SocialConfiguration {

    @Bean
    public ConnectionFactoryLocator connectionFactoryLocator(SocialConfigRepository socialConfigRepository) {
        return new DomainConnectionFactoryLocator(socialConfigRepository);
    }

    @Bean
    public UserIdSource userIdSource() {
        return new AuthenticationNameUserIdSource();
    }

    @Bean
    public UsersConnectionRepository usersConnectionRepository(
        SocialUserConnectionRepository socialUserConnectionRepository,
        ConnectionFactoryLocator connectionFactoryLocator) {
        return new CustomSocialUsersConnectionRepository(socialUserConnectionRepository, connectionFactoryLocator);
    }

    @Bean
    public SignInAdapter signInAdapter(UserDetailsService userDetailsService,
        AuthorizationServerTokenServices tokenServices) {
        return new CustomSignInAdapter(userDetailsService, tokenServices);
    }

    @Bean
    public ProviderSignInUtils providerSignInUtils(SessionStrategy sessionStrategy,
        ConnectionFactoryLocator connectionFactoryLocator) {
        return new ProviderSignInUtils(sessionStrategy, connectionFactoryLocator);
    }

    @Bean
    public ConnectSupport connectSupport(SessionStrategy sessionStrategy) {
        return new ConnectSupport(sessionStrategy);
    }

    @Bean
    public SessionStrategy sessionStrategy() {
        return new HttpSessionSessionStrategy();
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
    public ConnectionRepository connectionRepository(UsersConnectionRepository usersConnectionRepository) {
        return usersConnectionRepository.createConnectionRepository(userIdSource().getUserId());
    }

}
