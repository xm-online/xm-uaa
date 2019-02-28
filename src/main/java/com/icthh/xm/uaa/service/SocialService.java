package com.icthh.xm.uaa.service;

import static com.google.common.collect.ImmutableSet.of;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.NEED_ACCEPT_CONNECTION;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.REGISTERED;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.SING_IN;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.SocialUserConnection;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.repository.SocialUserConnectionRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.social.ConfigOAuth2ConnectionFactory;
import com.icthh.xm.uaa.social.ConfigServiceProvider;
import com.icthh.xm.uaa.social.SocialLoginAnswer;
import com.icthh.xm.uaa.social.SocialUserInfo;
import com.icthh.xm.uaa.social.SocialUserInfoMapper;
import com.icthh.xm.uaa.social.exceptions.FoundMoreThanOneUserBySocialUserInfo;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@LepService(group = "service.social")
public class SocialService {

    private final SocialUserConnectionRepository socialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AccountMailService accountMailService;
    private final UserDetailsService userDetailsService;

    private final TenantPropertiesService tenantPropertiesService;

    private final AuthorizationServerTokenServices tokenServices;
    private final UserLoginRepository userLoginRepository;
    protected final SocialUserInfoMapper socialUserInfoMapper;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;

    private SocialService self;

    public SocialService(SocialUserConnectionRepository socialRepository, PasswordEncoder passwordEncoder,
                         UserRepository userRepository, AccountMailService accountMailService,
                         UserDetailsService userDetailsService, TenantPropertiesService tenantPropertiesService,
                         AuthorizationServerTokenServices tokenServices, UserLoginRepository userLoginRepository,
                         SocialUserInfoMapper socialUserInfoMapper,
                         XmAuthenticationContextHolder xmAuthenticationContextHolder,
                         XmRequestContextHolder xmRequestContextHolder,
                         @Lazy SocialService self) {
        this.socialRepository = socialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.accountMailService = accountMailService;
        this.userDetailsService = userDetailsService;
        this.tenantPropertiesService = tenantPropertiesService;
        this.tokenServices = tokenServices;
        this.userLoginRepository = userLoginRepository;
        this.socialUserInfoMapper = socialUserInfoMapper;
        this.xmAuthenticationContextHolder = xmAuthenticationContextHolder;
        this.xmRequestContextHolder = xmRequestContextHolder;
        this.self = self;
    }

    @LogicExtensionPoint("InitSocialLogin")
    public String initSocialLogin(String providerId) {
        ConfigOAuth2ConnectionFactory connectionFactory = createConnectionFactory(providerId);
        OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
        OAuth2Parameters parameters = new OAuth2Parameters();
        parameters.setRedirectUri(redirectUri(providerId));
        parameters.setScope(connectionFactory.getScope());
        return oauthOperations.buildAuthenticateUrl(parameters);
    }

    private ConfigOAuth2ConnectionFactory createConnectionFactory(String providerId) {
        Social social = findSocialByProviderId(providerId);
        return new ConfigOAuth2ConnectionFactory(social, createConfigServiceProvider(social));
    }

    protected ConfigServiceProvider createConfigServiceProvider(Social social) {
        return new ConfigServiceProvider(social, socialUserInfoMapper);
    }

    private Social findSocialByProviderId(String providerId) {
        List<Social> socials = tenantPropertiesService.getTenantProps().getSocial();
        if (socials == null) {
            throw providerNotFound(providerId);
        }
        return socials.stream().filter(s -> s.getProviderId().equals(providerId)).findAny()
            .orElseThrow(() -> providerNotFound(providerId));
    }

    private String redirectUri(String providerId) {
        return Optional.ofNullable(tenantPropertiesService.getTenantProps().getSocialBaseUrl())
            .orElse(UaaUtils.getApplicationUrl(xmRequestContextHolder)) + "/uaa/social/signin/" + providerId;
    }

    private ProviderNotFoundException providerNotFound(String providerId) {
        return new ProviderNotFoundException(providerId + " not found");
    }

    @LogicExtensionPoint("AcceptSocialLogin")
    public SocialLoginAnswer acceptSocialLoginUser(String providerId, String code) {
        Social social = findSocialByProviderId(providerId);
        ConfigOAuth2ConnectionFactory connectionFactory = createConnectionFactory(providerId);
        OAuth2Operations oAuthOperations = connectionFactory.getOAuthOperations();
        AccessGrant accessGrant = oAuthOperations.exchangeForAccess(code, redirectUri(providerId), null);
        SocialUserInfo socialUserInfo = connectionFactory.createConnection(accessGrant).getApi().fetchSocialUser();

        String id = socialUserInfo.getId();
        Optional<SocialUserConnection> connection = socialRepository.findByProviderUserIdAndProviderId(id, providerId);
        if (connection.isPresent() && isNotBlank(connection.get().getUserKey())) {
            log.info("Found user social connection {}", connection.get());
            OAuth2AccessToken accessToken = self.signIn(connection.get().getUserKey());
            return SocialLoginAnswer.builder().answerType(SING_IN).oAuth2AccessToken(accessToken).build();
        }

        log.info("User social connection not found by providerUserid {} and providerId", id, providerId);

        List<User> existingUser = self.findUsersByUserInfo(socialUserInfo);
        if (existingUser.isEmpty()) {
            if (social.getCreateAccountAutomatically()) {
                User user = self.createSocialUser(socialUserInfo, providerId);
                createSocialConnection(socialUserInfo, user.getUserKey(), providerId);
                OAuth2AccessToken oAuth2AccessToken = signIn(user.getUserKey());
                return SocialLoginAnswer.builder().answerType(REGISTERED).oAuth2AccessToken(oAuth2AccessToken).build();
            } else {
                throw new NotImplementedException("Will be implemented in future");
            }
        } else if (existingUser.size() == 1) {
            SocialUserConnection userConnection = createSocialConnection(socialUserInfo, null, providerId);
            return SocialLoginAnswer.builder().answerType(NEED_ACCEPT_CONNECTION)
                .activationCode(userConnection.getActivationCode()).build();
        } else {
            throw new FoundMoreThanOneUserBySocialUserInfo(existingUser);
        }
    }

    @LogicExtensionPoint("AcceptConnection")
    public void acceptConnection(String activationCode) {
        Optional<SocialUserConnection> connection = socialRepository.findByActivationCode(activationCode);
        SocialUserConnection userConnection =
            connection.orElseThrow(() -> new EntityNotFoundException("User connection not found"));
        userConnection.setUserKey(xmAuthenticationContextHolder.getContext().getRequiredUserKey());
        userConnection.setActivationCode(null);
        socialRepository.save(userConnection);
    }

    private SocialUserConnection createSocialConnection(SocialUserInfo socialUserInfo, String userKey,
                                                        String providerId) {
        return socialRepository.save(
            new SocialUserConnection(null, userKey, providerId, socialUserInfo.getId(), socialUserInfo.getProfileUrl(),
                                     randomUUID().toString()));
    }

    @LogicExtensionPoint("SignIn")
    public OAuth2AccessToken signIn(String userKey) {
        User user = getUser(userKey);
        UserDetails userDetailts = userDetailsService.loadUserByUsername(getLogin(user));
        Authentication userAuth =
            new UsernamePasswordAuthenticationToken(userDetailts, "N/A", userDetailts.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(userAuth);
        OAuth2Request storedOAuth2Request =
            new OAuth2Request(null, "webapp", null, true, of("openid"), null, null, null, null);
        OAuth2Authentication oauth2 = new OAuth2Authentication(storedOAuth2Request, userAuth);
        return tokenServices.createAccessToken(oauth2);
    }

    @LogicExtensionPoint("FindUsersBySocialUserInfo")
    public List<User> findUsersByUserInfo(SocialUserInfo socialUserInfo) {
        return Stream.of(socialUserInfo.getEmail(), socialUserInfo.getPhoneNumber(), socialUserInfo.getUsername())
                     .filter(Objects::nonNull).map(String::valueOf).filter(StringUtils::isNotBlank)
                     .map(userLoginRepository::findOneByLoginIgnoreCase).filter(Objects::nonNull)
                     .filter(Optional::isPresent).map(Optional::get)
                     .map(UserLogin::getUser).collect(toList());
    }

    private String getLogin(User user) {
        return user.getLogins().stream().map(UserLogin::getLogin).filter(StringUtils::isNotBlank).findFirst()
                   .orElseThrow(() -> new BusinessException("error.not.logins.found", "All logins is null"));
    }

    @LogicExtensionPoint("CreateSocialUser")
    public User createSocialUser(SocialUserInfo userInfo, String providerId) {
        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));
        User newUser = new User();
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userInfo.getFirstName());
        newUser.setLastName(userInfo.getLastName());
        newUser.setUserKey(randomUUID().toString());
        newUser.setActivated(true);
        newUser.setRoleKey(tenantPropertiesService.getTenantProps().getSecurity().getDefaultUserRole());
        newUser.setLangKey(userInfo.getLangKey());
        newUser.setImageUrl(userInfo.getImageUrl());

        if (isNotBlank(userInfo.getEmail())) {
            UserLogin userLogin = new UserLogin();
            userLogin.setUser(newUser);
            userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
            userLogin.setLogin(userInfo.getEmail());
            newUser.getLogins().add(userLogin);
        }

        if (isNotBlank(userInfo.getPhoneNumber())) {
            UserLogin userLogin = new UserLogin();
            userLogin.setUser(newUser);
            userLogin.setTypeKey(UserLoginType.MSISDN.getValue());
            userLogin.setLogin(userInfo.getPhoneNumber());
            newUser.getLogins().add(userLogin);
        }

        if (isNotBlank(userInfo.getUsername())) {
            UserLogin userLogin = new UserLogin();
            userLogin.setUser(newUser);
            userLogin.setTypeKey(UserLoginType.NICKNAME.getValue());
            userLogin.setLogin(userInfo.getUsername());
            newUser.getLogins().add(userLogin);
        }

        log.info("Create user {}", newUser);

        User user = userRepository.save(newUser);
        accountMailService.sendSocialRegistrationValidationEmail(user, providerId);
        return user;
    }

    private User getUser(String userKey) {
        return userRepository.findOneByUserKey(userKey)
            .orElseThrow(() -> new EntityNotFoundException("User with key " + userKey + " not found"));
    }

    public void setSelf(SocialService self) {
        this.self = self;
    }
}
