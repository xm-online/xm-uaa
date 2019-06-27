package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.RegistrationLog;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Register new user.
     *
     * @param user user dto
     * @return new user
     */
    @Transactional
    @LogicExtensionPoint("Register")
    public User register(ManagedUserVM user, String ipAddress) {
        userService.validatePassword(user.getPassword());
        String encryptedPassword = passwordEncoder.encode(user.getPassword());

        User newUser = new User();
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setPassword(encryptedPassword);
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
            if (StringUtils.isNoneBlank(updatedUser.getRoleKey())) {
                user.setRoleKey(updatedUser.getRoleKey());
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
}
