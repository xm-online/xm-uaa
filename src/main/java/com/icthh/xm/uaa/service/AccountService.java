package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.GrantType;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.RegistrationLog;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSender;
import com.icthh.xm.uaa.security.oauth2.otp.OtpSenderFactory;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.dto.UserPassDto;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.util.OtpUtils;
import com.icthh.xm.uaa.web.rest.vm.AuthorizeUserVm;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.icthh.xm.uaa.config.Constants.AUTH_OPT_NOTIFICATION_KEY;
import static com.icthh.xm.uaa.config.Constants.OTP_SENDER_NOT_FOUND_ERROR_TEXT;

@LepService(group = "service.account")
@Transactional
@AllArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationLogRepository registrationLogRepository;
    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantPropertiesService tenantPropertiesService;
    private final UserService userService;
    private final UserLoginService userLoginService;
    private final ProfileEventProducer profileEventProducer;
    private final OtpSenderFactory otpSenderFactory;

    /**
     * Register new user.
     *
     * @param user user dto
     * @return new user
     */
    @Transactional
    @LogicExtensionPoint("Register")
    public User register(UserDTO user, String ipAddress) {
        UserPassDto userPassDto = getOrGeneratePassword(user);

        User newUser = new User();
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setPassword(userPassDto.getEncryptedPassword());
        newUser.setPasswordSetByUser(userPassDto.getSetByUser());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setImageUrl(user.getImageUrl());
        newUser.setLangKey(user.getLangKey());
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        newUser.setRoleKey(tenantPropertiesService.getTenantProps().getSecurity().getDefaultUserRole());
        newUser.setLogins(user.getLogins());
        newUser.getLogins().forEach(userLogin -> userLogin.setUser(newUser));
        newUser.setData(user.getData());

        User resultUser = userRepository.save(newUser);

        registrationLogRepository.save(new RegistrationLog(ipAddress));

        return resultUser;
    }

    public User registerUser(UserDTO user, String remoteAddress) {
        userLoginService.normalizeLogins(user.getLogins());
        userLoginService.verifyLoginsNotExist(user.getLogins());

        User newUser = register(user, remoteAddress);
        produceEvent(new UserDTO(newUser), Constants.CREATE_PROFILE_EVENT_TYPE);
        return newUser;
    }

    /**
     * Authorize new user and register if no password provided.
     *
     * @param authorizeUserVm user to authorize
     * @param remoteAddr remote address
     * @return authorization grant type
     */
    @Transactional
    @LogicExtensionPoint("Authorize")
    public String authorizeAccount(AuthorizeUserVm authorizeUserVm, String remoteAddr) {
        UserDTO userDTO = buildUserDtoWithLogin(authorizeUserVm);
        User user = userService.findOneByLogin(authorizeUserVm.getLogin())
            .orElseGet(() -> registerUser(userDTO, remoteAddr));

        if (user.getPasswordSetByUser() == Boolean.TRUE) {
            return GrantType.PASSWORD.getValue();
        } else {
            sendOtpCode(authorizeUserVm.getLogin(), user);
            return GrantType.OTP.getValue();
        }
    }

    public void sendOtpCode(String login) {
        User user = userService.findOneByLogin(login)
            .orElseThrow(() -> new BusinessException(String.format("User by login '%s' not found", login)));
        sendOtpCode(login, user);
    }

    private void sendOtpCode(String login, User user) {
        OtpUtils.validateOptCodeSentInterval(tenantPropertiesService.getTenantProps(), user.getOtpCodeCreationDate());

        OtpChannelType chanelType = OtpUtils.getOptChanelTypeByLogin(login);

        OtpSender sender = otpSenderFactory.getSender(chanelType)
            .orElseThrow(() -> new AuthenticationServiceException(
                OTP_SENDER_NOT_FOUND_ERROR_TEXT + chanelType.getTypeName()));

        String otpCode = new Totp(user.getTfaOtpSecret()).now();

        user.setOtpCode(otpCode);
        user.setOtpCodeCreationDate(Instant.now());
        userRepository.save(user);

        sender.send(new OtpSendDTO(otpCode, login, user.getUserKey(), getNotificationKeyByChannel(chanelType)));
    }

    /**
     * Activate user by key.
     *
     * @param key activation key
     * @return user
     */
    @LogicExtensionPoint("ActivateRegistration")
    public Optional<UserDTO> activateRegistration(String key) {
        return userRepository.findOneByActivationKey(key).map(this::activateUser).map(UserDTO::new);
    }

    private User activateUser(User user) {
        if (user.isActivated()) {
            throw new BusinessException("error.activation.code.used", "Activation code used");
        }

        Long activationKeyLifeTime = tenantPropertiesService.getTenantProps().getActivationKeyLifeTime();
        if (user.isActivated() || isKeyExpired(user, activationKeyLifeTime)) {
            throw new BusinessException("error.activation.code.expired", "Activation code expired");
        }

        user.setActivated(true);
        // for avoid second use of activation link in case when user retired, but activation link not expired
        user.setActivationKey(RandomUtil.generateActivationKey());
        return user;
    }

    private boolean isKeyExpired(User user, Long activationKeyLifeTime) {
        return activationKeyLifeTime != null
            && !activationKeyLifeTime.equals(0L)
            && user.getCreateActivationKeyDate().plusSeconds(activationKeyLifeTime).isBefore(Instant.now());
    }

    private String getRequiredUserKey() {
        XmAuthenticationContext context = authContextHolder.getContext();
        if (!context.isFullyAuthenticated()) {
            throw new IllegalStateException("Current UAA user is not fully authenticated");
        }

        return context.getUserKey()
            .orElseThrow(() -> new IllegalStateException("User key must be set for fully authenticated user"));
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     */
    @LogicExtensionPoint("UpdateAccount")
    public Optional<UserDTO> updateAccount(UserDTO updatedUser) {
        return userRepository.findOneWithLoginsByUserKey(getRequiredUserKey()).map(user -> {
            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setLangKey(updatedUser.getLangKey());
            user.setImageUrl(updatedUser.getImageUrl());
            if(CollectionUtils.isNotEmpty(updatedUser.getAuthorities())){
                user.setAuthorities(updatedUser.getAuthorities());
            }
            user.setData(updatedUser.getData());
            user.setAccessTokenValiditySeconds(updatedUser.getAccessTokenValiditySeconds());
            user.setRefreshTokenValiditySeconds(updatedUser.getRefreshTokenValiditySeconds());
            return userService.updateUserAutoLogoutSettings(updatedUser, user);
        }).map(UserDTO::new);
    }

    /**
     * Change account password.
     *
     * @param password password
     */
    @LogicExtensionPoint("ChangePassword")
    public UserDTO changePassword(ChangePasswordVM password) {
        userService.validatePassword(password.getNewPassword());
        Optional<User> userOpt = userRepository.findOneByUserKey(getRequiredUserKey());
        if (!userOpt.isPresent()) {
            throw new EntityNotFoundException("User not found");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Passed invalid old password");
        }
        String newPassword = passwordEncoder.encode(password.getNewPassword());
        user.setPassword(newPassword);
        userRepository.saveAndFlush(user);
        log.debug("Changed password for User: {}", user);
        return new UserDTO(user);
    }

    @Transactional
    public void enableTwoFactorAuth(TfaOtpChannelSpec otpChannelSpec) {
        userService.enableTwoFactorAuth(getRequiredUserKey(), otpChannelSpec);
    }

    @Transactional
    public void disableTwoFactorAuth() {
        userService.disableTwoFactorAuth(getRequiredUserKey());
    }

    @Transactional
    public Map<OtpChannelType, List<TfaOtpChannelSpec>> getTfaAvailableOtpChannelSpecs() {
        return userService.getTfaAvailableOtpChannelSpecs(getRequiredUserKey());
    }

    public void produceEvent(UserDTO userDto, String eventType) {
        String content = profileEventProducer.createEventJson(userDto, eventType);
        profileEventProducer.send(content);
    }

    private UserPassDto getOrGeneratePassword(UserDTO user) {
        if (user instanceof ManagedUserVM) {
            ManagedUserVM managedUser = (ManagedUserVM) user;
            userService.validatePassword(managedUser.getPassword());
            return new UserPassDto(passwordEncoder.encode(managedUser.getPassword()), true);
        }
        return new UserPassDto(passwordEncoder.encode(UUID.randomUUID().toString()), false);
    }

    private UserDTO buildUserDtoWithLogin(AuthorizeUserVm authorizeUserVm) {
        UserDTO userDTO = new UserDTO();
        userDTO.setLangKey(authorizeUserVm.getLangKey());
        userDTO.addUserLogin(authorizeUserVm.getLogin());
        return userDTO;
    }

    private TenantProperties.NotificationChannel getNotificationKeyByChannel(OtpChannelType chanelType) {
        TenantProperties.Notification notification = Optional.ofNullable(tenantPropertiesService.getTenantProps()
            .getCommunication().getNotifications().get(AUTH_OPT_NOTIFICATION_KEY))
            .orElseThrow(() -> new BusinessException("Authorize otp notification configuration is missing"));

       return Optional.ofNullable(notification.getChannels().get(chanelType.getTypeName()))
            .orElseThrow(() -> new BusinessException("Authorize otp notification channel is missing"));
    }
}
