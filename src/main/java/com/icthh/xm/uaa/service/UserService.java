package com.icthh.xm.uaa.service;

import com.google.common.base.Preconditions;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.uaa.config.ApplicationProperties;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings;
import com.icthh.xm.uaa.domain.properties.TenantProperties.PublicSettings.PasswordSettings;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserPermittedRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.TokenConstraintsService;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetHandlerFactory;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetRequest;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.dto.UserWithContext;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.util.OtpUtils;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.icthh.xm.uaa.service.util.RandomUtil.generateActivationKey;
import static com.icthh.xm.uaa.web.constant.ErrorConstants.*;
import static com.icthh.xm.uaa.web.rest.util.VerificationUtils.assertNotSuperAdmin;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Service class for managing users.
 */
@LepService(group = "service.user")
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final long DEFAULT_RESET_KEY_LIFETIME = 86400;

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMailService accountMailService;
    private final TenantPropertiesService tenantPropertiesService;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final UserPermittedRepository userPermittedRepository;
    private final TokenConstraintsService tokenConstraints;
    private final PasswordResetHandlerFactory passwordResetHandlerFactory;
    private final ApplicationProperties applicationProperties;
    private final TenantPermissionService tenantPermissionService;
    private final PermissionContextProvider permissionContextProvider;
    @Setter(onMethod = @__(@Autowired))
    private UserService self;

    public String getRequiredUserKey() {
        return xmAuthenticationContextHolder.getContext().getRequiredUserKey();
    }

    /**
     * Search user by reset key and set him password.
     *
     * @param newPassword new user password
     * @param key         user reset key
     * @return user with new password
     */
    @LoggingAspectConfig(inputExcludeParams = "newPassword")
    @LogicExtensionPoint("CompletePasswordReset")
    public User completePasswordReset(String newPassword, String key) {
        validatePassword(newPassword);
        return userRepository.findOneByResetKey(key)
            .map(this::checkResetKey)
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setPasswordSetByUser(true);
                user.setResetKey(null);
                user.setResetDate(null);
                user.setUpdatePasswordDate(Instant.now());
                return user;
            })
            .orElseThrow(() -> new BusinessException("error.reset.code.used", "Reset code used"));
    }

    @LogicExtensionPoint("AcceptTermsOfConditions")
    public User acceptTermsOfConditions(String acceptTocOneTimeToken) {
        return userRepository.findOneByAcceptTocOneTimeToken(acceptTocOneTimeToken)
            .map(user -> {
                user.setAcceptTocTime(Instant.now());
                user.setAcceptTocOneTimeToken(null);
                return user;
            })
            .orElseThrow(() -> new BusinessException("error.invalid.accept.terms.token", "Invalid token for accept terms fo conditions"));
    }

    /**
     * Search user by mail login and set him reset key.
     *
     * @param mail users mail login
     * @return user
     */
    @LogicExtensionPoint("RequestPasswordReset")
    public Optional<User> requestPasswordReset(String mail) {
        return requestPasswordResetForLoginWithType(mail, UserLoginType.EMAIL);
    }

    /**
     * Search user by login and set him reset key.
     *
     * @param login     users login
     * @param loginType login type
     * @return user
     */
    @LogicExtensionPoint("RequestPasswordResetForLoginWithType")
    public Optional<User> requestPasswordResetForLoginWithType(String login, UserLoginType loginType) {
        return userLoginRepository
            .findOneByLoginIgnoreCase(login)
            .filter(userLogin -> userLogin.getUser().isActivated()
                && loginType.getValue().equals(userLogin.getTypeKey()))
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
        newUser.setAuthorities(getRequiredRoleKey(user));
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        newUser.setPassword(encryptedPassword);
        newUser.setPasswordSetByUser(false);
        newUser.setResetKey(RandomUtil.generateResetKey());
        newUser.setResetDate(Instant.now());
        newUser.setActivated(true);
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setLogins(user.getLogins());
        newUser.getLogins().forEach(userLogin -> userLogin.setUser(newUser));
        newUser.setData(user.getData());
        newUser.setUpdatePasswordDate(Instant.now());

        return userRepository.save(updateUserAutoLogoutSettings(user, newUser));
    }


    /**
     * Update all information EXCEPT and [ROLE, STATE].
     *
     * @param updatedUser user to update
     * @return updated user
     */
    @LogicExtensionPoint("UpdateUser")
    public Optional<UserDTO> updateUser(UserDTO updatedUser) {
        return userRepository.findById(updatedUser.getId())
            .map(user -> mergeUserData(updatedUser, user))
            .map(user -> updateUserAutoLogoutSettings(updatedUser, user))
            .map(UserDTO::new);
    }

    /**
     * Blocks user account. Throws BusinessException.ERROR_USER_BLOCK_HIMSELF for self-block
     * @param userKey - user key
     * @return - user data
     */
    @LogicExtensionPoint("BlockUserAccount")
    public Optional<UserDTO> blockUserAccount(String userKey) {

        xmAuthenticationContextHolder.getContext().getUserKey().ifPresent(key -> {
            if (key.equals(userKey)) {
                throw new BusinessException(ERROR_USER_BLOCK_HIMSELF, "Forbidden to block himself");
            }
        });

        return changeUserAccountState(userKey, Boolean.FALSE);
    }

    /**
     * Activates user account. Throws BusinessException.ERROR_USER_BLOCK_HIMSELF for self-block
     * @param userKey - user key
     * @return - user data
     */
    @LogicExtensionPoint("ActivateUserAccount")
    public Optional<UserDTO> activateUserAccount(String userKey) {

        xmAuthenticationContextHolder.getContext().getUserKey().ifPresent(key -> {
            if (key.equals(userKey)) {
                throw new BusinessException(ERROR_USER_ACTIVATES_HIMSELF, "Forbidden to activate himself");
            }
        });

        return changeUserAccountState(userKey, Boolean.TRUE);
    }

    /**
     * Changes role for current user account with some restrictions.
     * 1. It is impossible to set EMPTY role
     * 2. It is impossible to change role for X to SUPER_ADMIN
     * 3. It is impossible to change role for SUPER_ADMIN to X
     * @param updatedUser - new user settings
     * @return updated user
     */
    @LogicExtensionPoint("ChangeUserRole")
    public Optional<UserDTO> changeUserRole(final UserDTO updatedUser) {
        Preconditions.checkArgument(isNotEmpty(updatedUser.getAuthorities()), "No roleKey provided");
        Preconditions.checkArgument(
            isNotEmpty(updatedUser.getAuthorities()
                .stream()
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toList())), "No roleKey provided");
        return userRepository.findById(updatedUser.getId())
            .map(user -> {
                assertNotSuperAdmin(user.getAuthorities());
                user.setAuthorities(updatedUser.getAuthorities());
                return user;
            })
            .map(UserDTO::new);
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
        deleteUser(userKey, userDTO -> log.info("No delete notification sent"));
    }

    /**
     * Delete user and call notification if successfully performed.
     *
     * @param userKey user key;
     */
    public void deleteUser(String userKey, Consumer<UserDTO> notification) {
        xmAuthenticationContextHolder.getContext().getUserKey().ifPresent(key -> {
            if (key.equals(userKey)) {
                throw new BusinessException(ERROR_USER_DELETE_HIMSELF, "Forbidden to delete himself");
            }
        });

        userRepository.findOneWithLoginsByUserKey(userKey).ifPresent(user -> {
            assertNotSuperAdmin(user.getAuthorities());
            userRepository.delete(user);
            notification.accept(new UserDTO(user));
        });
    }

    @Transactional(readOnly = true)
    @FindWithPermission("USER.GET_LIST")
    @PrivilegeDescription("Privilege to get all the users")
    public Page<UserDTO> getAllManagedUsers(Pageable pageable, String roleKey, String privilegeKey) {
        if (StringUtils.isNoneBlank(roleKey)) {
            log.debug("Find by roleKey {}", roleKey);
            return userPermittedRepository.findAllByRoleKey(pageable, roleKey, privilegeKey).map(UserDTO::new);
        } else {
            return userPermittedRepository.findAll(pageable, User.class, privilegeKey).map(UserDTO::new);
        }
    }

    /**
     * Get authenticated user account with actual permissions by authorities & permission authentication context
     * @return  UserDTO with auth context
     */
    public Optional<UserWithContext> getUserAccount() {
        String requiredUserKey = getRequiredUserKey();
        return findOneWithLoginsByUserKey(requiredUserKey)
            .map(user -> {
                UserWithContext userDto = new UserWithContext(user);
                userDto.getPermissions().addAll(tenantPermissionService.getEnabledPermissionByRole(user.getAuthorities()));
                userDto.setContext(permissionContextProvider.getPermissionContext(requiredUserKey));
                return userDto;
            });
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
        return userRepository.findById(id).orElse(null);
    }

    @LogicExtensionPoint("SaveUser")
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @LogicExtensionPoint("ResetActivationKey")
    public void resetActivationKey(String login) {
        UserLogin userLogin = userLoginRepository.findOneByLoginIgnoreCase(login)
            .orElseThrow(
                () -> new BusinessException("error.user.notfound", String.format("User by login %s not found", login)));
        User user = userLogin.getUser();
        user.setActivationKey(generateActivationKey());
        accountMailService.sendMailOnRegistration(user);
        userRepository.save(user);
    }


    @LogicExtensionPoint("CheckPasswordReset")
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
    @LogicExtensionPoint(("FindOneByLogin"))
    public Optional<User> findOneByLogin(String login) {
        return userLoginRepository.findOneByLoginIgnoreCase(login).map(UserLogin::getUser);
    }

    private List<String> getRequiredRoleKey(UserDTO user) {
        return CollectionUtils.isEmpty(user.getAuthorities()) ?
            List.of(tenantPropertiesService.getTenantProps().getSecurity().getDefaultUserRole()) :
            user.getAuthorities();
    }

    @Transactional
    public void enableTwoFactorAuth(Long userId, TfaOtpChannelSpec otpChannelSpec) {
        // Check is user exist
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

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

        if (anyLoginForChannel.isEmpty()) {
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

    public Map<OtpChannelType, List<TfaOtpChannelSpec>> getTfaAvailableOtpChannelSpecs(String userKey) {
        User user = userRepository.findOneWithLoginsByUserKey(userKey).orElseThrow(
            () -> new EntityNotFoundException("User not found")
        );

        Set<OtpChannelType> tenantOtpChannelTypes = tenantPropertiesService.getTenantProps().getSecurity().getTfaEnabledOtpChannelTypes();

        Map<OtpChannelType, List<TfaOtpChannelSpec>> result = new HashMap<>();
        for (UserLogin userLogin : user.getLogins()) {
            // is channel type supported by current login type
            UserLoginType loginType = OtpUtils.getRequiredLoginType(userLogin.getTypeKey());
            Optional<List<OtpChannelType>> userOtpChannelTypes = OtpUtils.getSupportedOtpChannelTypes(loginType);
            if (userOtpChannelTypes.isEmpty()) {
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


    /**
     * Changes user account state. Method is private by design.
     * @param userKey user key
     * @param newState new state (true = active)
     * @return Optional<UserDTO>
     */
    private Optional<UserDTO> changeUserAccountState(String userKey, boolean newState) {
        return userRepository
            .findOneByUserKey(userKey)
            .map(user -> {
                user.setActivated(newState);
                return user;
            })
            .map(UserDTO::new);
    }

    public List<User> findAll(Specification<User> specification) {
        return userRepository.findAll(specification);
    }

    public Page<User> findAll(Specification<User> specification, Pageable pageable) {
        return userRepository.findAll(specification, pageable);
    }

    @LogicExtensionPoint("ValidatePassword")
    public void validatePassword(String password) throws BusinessException {
        PublicSettings publicSettings = tenantPropertiesService.getTenantProps()
                                                                .getPublicSettings();
        if (publicSettings == null) {
            return;
        }

        PasswordSettings passwordSettings = publicSettings.getPasswordSettings();
        if (passwordSettings == null || !passwordSettings.isEnableBackEndValidation()) {
            return;
        }

        validatePasswordPattern(password, passwordSettings);
        validatePasswordMinLength(password, passwordSettings);
        validatePasswordMaxLength(password, passwordSettings);
        validatePasswordPolicies(password, publicSettings.getPasswordPolicies(), publicSettings.getPasswordPoliciesMinimalMatchCount());
    }

    @LogicExtensionPoint("FindAllByLoginContains")
    @Transactional(readOnly = true)
    public Page<User> findAllByLoginContains(String login, Pageable pageable) {
        return userLoginRepository.findAllByLoginContainingIgnoreCase(login, pageable)
              .map(UserLogin::getUser);
    }

    private void validatePasswordPattern(String password, PasswordSettings passwordSettings) {
        if (isEmpty(passwordSettings.getPattern())) {
            return;
        }

        if (!validatePasswordPattern(password, passwordSettings.getPattern())) {
            throw new BusinessException("password.validation.failed",
                                        "password doesn't match regex");
        }
    }

    private boolean validatePasswordPattern(String password, String passwordPattern) {
        Pattern pattern = Pattern.compile(passwordPattern);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private void validatePasswordPolicies(String password, List<PublicSettings.PasswordPolicy> passwordPolicies,
                                          Long passwordPoliciesMinimalMatchCount) {
        if (passwordPolicies == null || passwordPolicies.isEmpty()) {
            return;
        }
        long countOfMatchedPolicies = passwordPolicies.stream()
            .map(passwordPolicy -> validatePasswordPattern(password, passwordPolicy.getPattern()))
            .filter(Boolean.TRUE::equals)
            .count();

        if (countOfMatchedPolicies < passwordPoliciesMinimalMatchCount) {
            throw new BusinessException("password.validation.failed",
                                        "password doesn't matched required count of policies");
        }
    }

    private void validatePasswordMinLength(String password, PasswordSettings passwordSettings) {
        if (password.length() < passwordSettings.getMinLength()) {
            throw new BusinessException("password.validation.failed",
                                        "password length is less than the minimum");
        }
    }

    private void validatePasswordMaxLength(String password, PasswordSettings passwordSettings) {
        if (password.length() > passwordSettings.getMaxLength()) {
            throw new BusinessException("password.validation.failed",
                                        "password length is greater than the maximum");
        }
    }

    protected User mergeUserData(UserDTO srcDTO, User dstUser) {

        //if isStrictUserManagement do not change role
        if (tenantPropertiesService.getTenantProps().isStrictUserManagement()) {
            //use original user state
            dstUser.setActivated(dstUser.isActivated());
            //use original user role
            dstUser.setAuthorities(dstUser.getAuthorities());
        } else {

            //role update case
            if (!isEqualCollection(dstUser.getAuthorities(), srcDTO.getAuthorities())) {
                if (CollectionUtils.isEmpty(srcDTO.getAuthorities())) {
                    log.warn("Role is empty and will not be allied to user");
                } else {
                    log.warn("Role [{}] will be allied to user.id={}. Evaluate strictUserManagement as option", srcDTO.getAuthorities(), dstUser.getId());
                    dstUser.setAuthorities(srcDTO.getAuthorities());
                }
            }

            if (dstUser.isActivated() != srcDTO.isActivated()) {
                log.warn("State isActivated=[{}] will be allied to user.id={}. Evaluate strictUserManagement as option", srcDTO.isActivated(), dstUser.getId());
                dstUser.setActivated(srcDTO.isActivated());
            }

        }

        dstUser.setFirstName(srcDTO.getFirstName());
        dstUser.setLastName(srcDTO.getLastName());
        dstUser.setLangKey(srcDTO.getLangKey());
        dstUser.setImageUrl(srcDTO.getImageUrl());
        dstUser.setData(srcDTO.getData());
        dstUser.setAccessTokenValiditySeconds(srcDTO.getAccessTokenValiditySeconds());
        dstUser.setRefreshTokenValiditySeconds(srcDTO.getRefreshTokenValiditySeconds());
        return  dstUser;
    }

    // dstUser need to have logout timeout data
    protected User updateUserAutoLogoutSettings(UserDTO srcDTO, User dstUser) {
        Integer srcAutoLogoutTimeoutSeconds = srcDTO.getAutoLogoutTimeoutSeconds();
        if (srcAutoLogoutTimeoutSeconds != null) {
            int accessTokenValiditySeconds = tokenConstraints.getAccessTokenValiditySeconds(dstUser.getAccessTokenValiditySeconds());
            if (srcAutoLogoutTimeoutSeconds > accessTokenValiditySeconds) {
                srcAutoLogoutTimeoutSeconds = accessTokenValiditySeconds;
            }

            if (srcAutoLogoutTimeoutSeconds <= 0) {
                srcAutoLogoutTimeoutSeconds = null;
            }
        }

        dstUser.setAutoLogoutEnabled(srcDTO.isAutoLogoutEnabled());
        dstUser.setAutoLogoutTimeoutSeconds(srcAutoLogoutTimeoutSeconds);
        return dstUser;
    }

    @Transactional(propagation = REQUIRES_NEW)
    public User updateAcceptTermsOfConditionsToken(User user) {
        String token = randomUUID().toString();
        user.setAcceptTocOneTimeToken(token);
        return userRepository.save(user);
    }

    public User updateLastLoginDate(User user) {
        user.setLastLoginDate(Instant.now());
        return userRepository.save(user);
    }

    public void onSuccessfulLogin(String userKey) {
        Integer maxPasswordAttempts = tenantPropertiesService.getTenantProps().getSecurity().getMaxPasswordAttempts();

        userRepository.findOneByUserKey(userKey).ifPresent(user -> {
            if (applicationProperties.isLastLoginDateEnabled()) {
                user.updateLastLoginDate();
            }

            if (maxPasswordAttempts != null && maxPasswordAttempts > 0) {
                user.resetPasswordAttempts();
            }
        });
    }

    @LogicExtensionPoint(value = "IncreaseFailedPasswordAttempts")
    public void increaseFailedPasswordAttempts(String username) {
        Integer maxPasswordAttempts = tenantPropertiesService.getTenantProps().getSecurity().getMaxPasswordAttempts();

        if (maxPasswordAttempts != null && maxPasswordAttempts > 0) {
            userLoginRepository.findOneByLoginIgnoreCase(username)
                .map(UserLogin::getUser)
                .map(User::incrementPasswordAttempts)
                .filter(user -> user.getPasswordAttempts().equals(maxPasswordAttempts))
                .ifPresent(self::passwordAttemptsExceeded);
        }
    }

    public void handlePasswordReset(PasswordResetRequest resetRequest) {
        passwordResetHandlerFactory.getPasswordResetHandler(resetRequest.getResetType()).handle(resetRequest);
    }

    @LogicExtensionPoint(value = "PasswordAttemptsExceeded")
    public void passwordAttemptsExceeded(User user) {
        user.setActivated(false);
        user.resetPasswordAttempts();
    }
}
