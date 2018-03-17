package com.icthh.xm.uaa.service;

import static com.icthh.xm.uaa.service.util.RandomUtil.generateActivationKey;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.ERROR_USER_DELETE_HIMSELF;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserPermittedRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.util.OtpUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service class for managing users.
 */
@LepService(group = "service.user")
@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UserService {

    private static final long DEFAULT_RESET_KEY_LIFETIME = 86400;

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocialService socialService;
    private final AccountMailService accountMailService;
    private final TenantPropertiesService tenantPropertiesService;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final UserPermittedRepository userPermittedRepository;
    private final TokenConstraintsService tokenConstraints;

    /**
     * Search user by reset key and set him password.
     *
     * @param newPassword new user password
     * @param key         user reset key
     * @return user with new password
     */
    @LoggingAspectConfig(inputExcludeParams = "newPassword")
    public User completePasswordReset(String newPassword, String key) {
        return userRepository.findOneByResetKey(key)
            .map(this::checkResetKey)
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                return user;
            })
            .orElseThrow(() -> new BusinessException("error.reset.code.used", "Reset code used"));
    }

    /**
     * Search user by mail login and set him reset key.
     *
     * @param mail users mail login
     * @return user
     */
    @LogicExtensionPoint("RequestPasswordReset")
    public Optional<User> requestPasswordReset(String mail) {
        return userLoginRepository
            .findOneByLoginIgnoreCase(mail)
            .filter(userLogin -> userLogin.getUser().isActivated()
                && UserLoginType.EMAIL.getValue().equals(userLogin.getTypeKey()))
            .map(userLogin -> {
                userLogin.getUser().setResetKey(RandomUtil.generateResetKey());
                userLogin.getUser().setResetDate(Instant.now());
                return userLogin.getUser();
            });
    }

    /**
     * Create new user by admin.
     *
     * @param user new user
     * @return user
     */
    @LogicExtensionPoint("CreateUser")
    public User createUser(UserDTO user) {
        User newUser = new User();
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setImageUrl(user.getImageUrl());
        newUser.setLangKey(user.getLangKey() == null ? "en" : user.getLangKey());
        newUser.setRoleKey(getRequiredRoleKey(user));
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        newUser.setPassword(encryptedPassword);
        newUser.setResetKey(RandomUtil.generateResetKey());
        newUser.setResetDate(Instant.now());
        newUser.setActivated(true);
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setLogins(user.getLogins());
        newUser.getLogins().forEach(userLogin -> userLogin.setUser(newUser));
        newUser.setData(user.getData());
        userRepository.save(newUser);
        return newUser;
    }


    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param updatedUser user to update
     * @return updated user
     */
    @LogicExtensionPoint("UpdateUser")
    public Optional<UserDTO> updateUser(UserDTO updatedUser) {
        return Optional.of(userRepository
                               .findOne(updatedUser.getId()))
            .map(user -> {
                user.setFirstName(updatedUser.getFirstName());
                user.setLastName(updatedUser.getLastName());
                user.setLangKey(updatedUser.getLangKey());
                user.setImageUrl(updatedUser.getImageUrl());
                user.setActivated(updatedUser.isActivated());
                if (StringUtils.isNoneBlank(updatedUser.getRoleKey())) {
                    user.setRoleKey(updatedUser.getRoleKey());
                }
                user.setData(updatedUser.getData());
                user.setAccessTokenValiditySeconds(updatedUser.getAccessTokenValiditySeconds());
                user.setRefreshTokenValiditySeconds(updatedUser.getRefreshTokenValiditySeconds());
                return updateUserAutoLogoutSettings(updatedUser, user);
            })
            .map(UserDTO::new);
    }

    // dstUser need to have logout timeout data
    User updateUserAutoLogoutSettings(UserDTO srcDTO, User dstUser) {
        Integer srcAutoLogoutTimeoutSeconds = srcDTO.getAutoLogoutTimeoutSeconds();
        if (srcAutoLogoutTimeoutSeconds != null) {
            int accessTokenValiditySeconds = tokenConstraints.getAccessTokenValiditySeconds(dstUser.getAutoLogoutTimeoutSeconds());
            if (srcAutoLogoutTimeoutSeconds > accessTokenValiditySeconds) {
                srcAutoLogoutTimeoutSeconds = accessTokenValiditySeconds;
            }

            if (srcAutoLogoutTimeoutSeconds <= 0) {
                srcAutoLogoutTimeoutSeconds = null;
            }
        }

        dstUser.setAutoLogoutEnabled(srcDTO.isTfaEnabled());
        dstUser.setAutoLogoutTimeoutSeconds(srcAutoLogoutTimeoutSeconds);
        return dstUser;
    }

    /**
     * Update logins for a specific user, and return the modified user.
     *
     * @param userKey user key
     * @param logins  the list of new logins
     * @return new user
     */
    public Optional<UserDTO> updateUserLogins(String userKey, List<UserLogin> logins) {
        Optional<User> f = userRepository
            .findOneByUserKey(userKey)
            .map(user -> {
                List<UserLogin> userLogins = user.getLogins();
                userLogins.clear();
                logins.forEach(userLogin -> userLogin.setUser(user));
                userLogins.addAll(logins);
                return user;
            });
        f.ifPresent(userRepository::save);
        return f.map(UserDTO::new);
    }

    /**
     * Delete user.
     *
     * @param userKey user key;
     */
    public void deleteUser(String userKey) {
        if (xmAuthenticationContextHolder.getContext().getRequiredUserKey().equals(userKey)) {
            throw new BusinessException(ERROR_USER_DELETE_HIMSELF, "Forbidden to delete himself");
        }
        userRepository.findOneWithLoginsByUserKey(userKey).ifPresent(user -> {
            socialService.deleteUserSocialConnection(user.getUserKey());
            userRepository.delete(user);
        });
    }

    @Transactional(readOnly = true)
    @FindWithPermission("USER.GET_LIST")
    public Page<UserDTO> getAllManagedUsers(Pageable pageable, String roleKey, String privilegeKey) {
        if (StringUtils.isNoneBlank(roleKey)) {
            log.debug("Find by roleKey {}", roleKey);
            return userPermittedRepository.findAllByRoleKey(pageable, roleKey, privilegeKey).map(UserDTO::new);
        } else {
            return userPermittedRepository.findAll(pageable, User.class, privilegeKey).map(UserDTO::new);
        }
    }

    /**
     * Search user by user key.
     *
     * @param userKey user key
     * @return user
     */
    @LogicExtensionPoint("FindOneWithLoginsByUserKey")
    @Transactional(readOnly = true)
    public Optional<User> findOneWithLoginsByUserKey(String userKey) {
        if (StringUtils.isBlank(userKey)) {
            log.warn("User key is empty");
            return Optional.empty();
        }
        return userRepository.findOneWithLoginsByUserKey(userKey);
    }

    @Transactional(readOnly = true)
    public User getUser(String userKey) {
        return userRepository.findOneByUserKey(userKey).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findOne(id);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void resetActivationKey(String login) {
        UserLogin userLogin = userLoginRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(
                () -> new BusinessException("error.user.notfound", String.format("User by login %s not found", login)));
        User user = userLogin.getUser();
        user.setActivationKey(generateActivationKey());
        accountMailService.sendMailOnRegistration(user);
        userRepository.save(user);
    }

    public void checkPasswordReset(String key) {
        userRepository.findOneByResetKey(key).map(this::checkResetKey)
            .orElseThrow(() -> new BusinessException("error.reset.code.used", "Reset code used"));
    }

    private User checkResetKey(User user) {
        Long resetKeyLifeTime = Optional.ofNullable(tenantPropertiesService.getTenantProps().getResetKeyLifeTime())
            .orElse(DEFAULT_RESET_KEY_LIFETIME);
        if (isKeyExpired(user, resetKeyLifeTime)) {
            throw new BusinessException("error.reset.code.expired", "Reset code expired");
        }
        return user;
    }

    private boolean isKeyExpired(User user, Long resetKeyLifeTime) {
        if (resetKeyLifeTime == null || resetKeyLifeTime == 0L || user.getResetDate() == null) {
            return false;
        }
        return user.getResetDate().plusSeconds(resetKeyLifeTime).isBefore(Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<User> findOneByLogin(String login) {
        return userLoginRepository.findOneByLoginIgnoreCase(login).map(UserLogin::getUser);
    }

    private String getRequiredRoleKey(UserDTO user) {
        return StringUtils.isBlank(user.getRoleKey()) ? tenantPropertiesService.getTenantProps()
            .getSecurity().getDefaultUserRole() : user.getRoleKey();
    }

    @Transactional
    public void enableTwoFactorAuth(Long userId, TfaOtpChannelSpec otpChannelSpec) {
        // Check is user exist
        User user = userRepository.findOne(userId);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        enableTwoFactorAuth(user, otpChannelSpec);
    }

    @Transactional
    public void enableTwoFactorAuth(String userKey, TfaOtpChannelSpec otpChannelSpec) {
        // Check is user exist
        User user = userRepository.findOneWithLoginsByUserKey(userKey).orElseThrow(
            () -> new EntityNotFoundException("User not found")
        );

        enableTwoFactorAuth(user, otpChannelSpec);
    }

    private void enableTwoFactorAuth(User user, TfaOtpChannelSpec otpChannelSpec) {
        // 1. Check is tenant allow TFA ?
        if (!tenantPropertiesService.getTenantProps().getSecurity().isTfaEnabled()) {
            throw new BusinessException("TFA is disabled for current tenant");
        }

        final OtpChannelType otpChannelType = otpChannelSpec.getChannelType();
        final String channelTypeName = otpChannelType.getTypeName();

        // 2. Check is tenant allow specified OTP channel type ?
        Set<OtpChannelType> tfaEnabledOtpChannelTypes = tenantPropertiesService.getTenantProps().getSecurity().getTfaEnabledOtpChannelTypes();
        if (!CollectionUtils.isEmpty(tfaEnabledOtpChannelTypes)
            && !tfaEnabledOtpChannelTypes.contains(otpChannelType)) {
            throw new BusinessException("Current tenant is not supported TFA OTP channel type '" + channelTypeName + "'");
        }

        // 3. Check is user has login appropriate to channel spec
        // TODO this check must be replaced by separate channel spec verification process
        Optional<UserLogin> anyLoginForChannel = user.getLogins().stream().filter(userLogin -> {
            // is login equals to channel destination
            if (!Objects.equals(otpChannelSpec.getDestination(), userLogin.getLogin())) {
                return false;
            }

            // is channel type supported by current login type
            UserLoginType loginType = OtpUtils.getRequiredLoginType(userLogin.getTypeKey());
            Optional<List<OtpChannelType>> supportedOtpChannelTypes = OtpUtils.getSupportedOtpChannelTypes(loginType);
            return supportedOtpChannelTypes.map(otpChannelTypes -> otpChannelTypes.contains(otpChannelType)).orElse(false);
        }).findAny();

        if (!anyLoginForChannel.isPresent()) {
            throw new BusinessException("User has no login for OTP channel type: " + channelTypeName);
        }

        // 4. Enable TFA for User and set channel type
        user.setTfaEnabled(true);
        user.setTfaOtpChannelType(otpChannelType);

        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactorAuth(String userKey) {
        // Check is user exist
        User user = userRepository.findOneWithLoginsByUserKey(userKey).orElseThrow(
            () -> new EntityNotFoundException("User not found")
        );
        user.setTfaEnabled(false);

        userRepository.save(user);
    }

    private static Optional<List<OtpChannelType>> getUserLoginOtpChannelTypes(UserLogin userLogin) {
        UserLoginType loginType = OtpUtils.getRequiredLoginType(userLogin.getTypeKey());
        return OtpUtils.getSupportedOtpChannelTypes(loginType);
    }

    public Map<OtpChannelType, List<TfaOtpChannelSpec>> getTfaAvailableOtpChannelSpecs(String userKey) {
        User user = userRepository.findOneWithLoginsByUserKey(userKey).orElseThrow(
            () -> new EntityNotFoundException("User not found")
        );

        Set<OtpChannelType> tenantOtpChannelTypes = tenantPropertiesService.getTenantProps().getSecurity().getTfaEnabledOtpChannelTypes();

        Map<OtpChannelType, List<TfaOtpChannelSpec>> result = new HashMap<>();
        for (UserLogin userLogin : user.getLogins()) {
            // is channel type supported by current login type
            Optional<List<OtpChannelType>> userOtpChannelTypes = getUserLoginOtpChannelTypes(userLogin);
            if (!userOtpChannelTypes.isPresent()) {
                continue;
            }

            Set<OtpChannelType> intersection = new HashSet<>(tenantOtpChannelTypes);
            intersection.retainAll(userOtpChannelTypes.get());
            for (OtpChannelType channelType : intersection) {
                TfaOtpChannelSpec channelSpec = new TfaOtpChannelSpec(channelType, userLogin.getLogin());

                List<TfaOtpChannelSpec> otpChannelSpecs = result.computeIfAbsent(channelType, otpChannelType -> new LinkedList<>());
                otpChannelSpecs.add(channelSpec);
            }
        }


        return result;
    }

}
