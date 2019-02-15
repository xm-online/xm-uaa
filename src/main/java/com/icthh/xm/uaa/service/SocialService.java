package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties.Social;
import com.icthh.xm.uaa.repository.SocialUserConnectionRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.mail.MailService;
import com.icthh.xm.uaa.social.ConfigOAuth2ConnectionFactory;
import com.icthh.xm.uaa.social.SocialUserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SocialService {

    private final SocialUserConnectionRepository socialUserConnectionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final UserLoginRepository userLoginRepository;
    private final TenantContextHolder tenantContextHolder;
    private final XmRequestContextHolder requestContextHolder;
    private final TenantPropertiesService tenantPropertiesService;

    public String initSocialLogin(String providerId) {
        ConfigOAuth2ConnectionFactory connectionFactory = createConnectionFactory(providerId);
        OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
        String scope = connectionFactory.getScope();
        OAuth2Parameters parameters = new OAuth2Parameters();
        parameters.setRedirectUri(getRedirectUri());
        parameters.setScope(scope);
        String state = connectionFactory.generateState();
        parameters.add("state", state);
        return oauthOperations.buildAuthenticateUrl(parameters);
    }

    private ConfigOAuth2ConnectionFactory createConnectionFactory(String providerId) {
        List<Social> socials = tenantPropertiesService.getTenantProps().getSocial();
        if (socials == null) {
            throw providerNotFound(providerId);
        }
        Social social = socials.stream()
                               .filter(s -> s.getProviderId().equals(providerId))
                               .findAny()
                               .orElseThrow(() -> providerNotFound(providerId));
        return new ConfigOAuth2ConnectionFactory(social);
    }

    private String getRedirectUri() {
        return "http://localhost:8080/google/login";
    }

    private ProviderNotFoundException providerNotFound(String providerId) {
        return new ProviderNotFoundException(providerId + " not found");
    }

    public void loginUser(String providerId, String code) {
        ConfigOAuth2ConnectionFactory connectionFactory = createConnectionFactory(providerId);
        AccessGrant accessGrant = connectionFactory.getOAuthOperations()
                                                   .exchangeForAccess(code, getRedirectUri(), null);
        SocialUserDto socialUserDto = connectionFactory.createConnection(accessGrant).getApi().fetchSocialUser();
        socialUserConnectionRepository.findByProviderUserIdAndAndProviderId(socialUserDto.getId(), providerId);

    }

    /**
     * Create new user.
     *
     * @param connection connection
     * @param langKey    lang key
     */
    public void createSocialUser(Connection<?> connection, String langKey) {
        if (connection == null) {
            log.error("Cannot create social user because connection is null");
            throw new IllegalArgumentException("Connection cannot be null");
        }

        UserProfile userProfile = connection.fetchUserProfile();
        String providerId = connection.getKey().getProviderId();
        String imageUrl = connection.getImageUrl();
        User user = createUserIfNotExist(userProfile, langKey, imageUrl);

        mailService.sendSocialRegistrationValidationEmail(user, userProfile.getEmail(), providerId,
                                                          UaaUtils.getApplicationUrl(requestContextHolder),
                                                          TenantContextUtils.getRequiredTenantKey(tenantContextHolder),
                                                          MdcUtils.getRid());
    }

    private User createUserIfNotExist(UserProfile userProfile, String langKey, String imageUrl) {
        String email = userProfile.getEmail();
        if (StringUtils.isBlank(email)) {
            log.error("Cannot create social user because email is null");
            throw new IllegalArgumentException("Email cannot be null");
        } else {
            Optional<UserLogin> user = userLoginRepository.findOneByLoginIgnoreCase(email);
            if (user.isPresent()) {
                log.info("User already exist associate the connection to this account");
                return user.get().getUser();
            }
        }

        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));
        User newUser = new User();
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userProfile.getFirstName());
        newUser.setLastName(userProfile.getLastName());
        newUser.setActivated(true);
        newUser.setRoleKey(tenantPropertiesService.getTenantProps().getSecurity().getDefaultUserRole());
        newUser.setLangKey(langKey);
        newUser.setImageUrl(imageUrl);

        UserLogin userLogin = new UserLogin();
        userLogin.setUser(newUser);
        userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
        userLogin.setLogin(email);
        newUser.getLogins().add(userLogin);

        return userRepository.save(newUser);
    }

    void deleteUserSocialConnection(String userKey) {
        //TODO
    }

}
