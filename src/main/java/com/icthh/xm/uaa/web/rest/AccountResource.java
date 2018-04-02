package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.uaa.config.Constants.LOGIN_IS_USED_ERROR_TEXT;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.OtpChannelType;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.service.AccountMailService;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.CaptchaService;
import com.icthh.xm.uaa.service.TenantPermissionService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.TfaEnableRequest;
import com.icthh.xm.uaa.service.dto.TfaOtpChannelSpec;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.rest.util.HeaderUtil;
import com.icthh.xm.uaa.web.rest.vm.CaptchaVM;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.KeyAndPasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import io.github.jhipster.web.util.ResponseUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
@Slf4j
@AllArgsConstructor
@SuppressWarnings("unused")
public class AccountResource {

    private static final String CHECK_ERROR_MESSAGE = "Incorrect password";
    private static final String LOGIN_USED_CODE = "error.login.already.used";
    private static final String LOGIN_USED_PARAM = "loginTypeKey";

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final ProfileEventProducer profileEventProducer;
    private final CaptchaService captchaService;
    private final XmAuthenticationContextHolder xmAuthenticationContextHolder;
    private final XmRequestContextHolder xmRequestContextHolder;
    private final TenantContextHolder tenantContextHolder;
    private final TenantPermissionService tenantPermissionService;
    private final AccountMailService accountMailService;

    private String getRequiredUserKey() {
        return xmAuthenticationContextHolder.getContext().getRequiredUserKey();
    }

    /**
     * POST /register : register the user.
     *
     * @param user the managed user View Model
     * @return the ResponseEntity with status 201 (Created) if the user is registered or 400 (Bad Request) if the login
     * is already in use
     */
    @PostMapping(path = "/register", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Timed
    @PreAuthorize("hasPermission({'user': #user, 'request': #request}, 'ACCOUNT.REGISTER')")
    public ResponseEntity<Void> registerAccount(@Valid @RequestBody ManagedUserVM user, HttpServletRequest request) {
        if (user.getEmail() == null) {
            throw new BusinessException("Email can't be empty");
        }
        user.getLogins().forEach(
            userLogin -> userLoginRepository.findOneByLoginIgnoreCase(userLogin.getLogin()).ifPresent(s -> {
                Map<String, String> params = new HashMap<>();
                params.put(LOGIN_USED_PARAM, s.getTypeKey());
                throw new BusinessException(LOGIN_USED_CODE, LOGIN_IS_USED_ERROR_TEXT, params);
            }));
        if (captchaService.isCaptchaNeed(request.getRemoteAddr())) {
            captchaService.checkCaptcha(user.getCaptcha());
        }
        User newUser = accountService.register(user, request.getRemoteAddr());
        produceEvent(new UserDTO(newUser), Constants.CREATE_PROFILE_EVENT_TYPE);
        accountMailService.sendMailOnRegistration(newUser);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/is-captcha-need")
    @Timed
    @PreAuthorize("hasPermission({'request': #request}, 'CAPTCHA.GET')")
    public ResponseEntity<CaptchaVM> isCaptchaNeed(HttpServletRequest request) {
        return ResponseEntity.ok(new CaptchaVM(
            captchaService.isCaptchaNeed(request.getRemoteAddr()),
            captchaService.getPublicKey()
        ));
    }

    /**
     * GET /activate : activate the registered user.
     *
     * @param key the activation key
     * @return the ResponseEntity with status 200 (OK) and the activated user in body, or status 500 (Internal Server
     * Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    @PreAuthorize("hasPermission(null, 'ACCOUNT.ACTIVATE')")
    public ResponseEntity<String> activateAccount(@RequestParam("key") String key) {
        return accountService.activateRegistration(key)
            .map(user -> {
                produceEvent(user, Constants.ACTIVATE_PROFILE_EVENT_TYPE);
                return new ResponseEntity<String>(HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    @PreAuthorize("hasPermission({'request': #request}, 'ACCOUNT.CHECK_AUTH')")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET /account : get the current user.
     *
     * @return the ResponseEntity with status 200 (OK) and the current user in body, or status 500 (Internal Server
     * Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ACCOUNT.GET_LIST.ITEM')")
    public ResponseEntity<UserDTO> getAccount() {
        return ResponseUtil.wrapOrNotFound(userService.findOneWithLoginsByUserKey(getRequiredUserKey())
                                               .map(user -> {
                                                   UserDTO userDto = new UserDTO(user);
                                                   userDto.getPermissions().addAll(tenantPermissionService.getEnabledPermissionByRole(user.getRoleKey()));
                                                   return userDto;
                                               }));
    }

    @PostMapping(path = "/account/reset_activation_key",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'login': #login}, 'ACCOUNT.RESET_ACTIVATION')")
    public ResponseEntity<Void> resetActivationKey(@RequestBody String login) {
        userService.resetActivationKey(login);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account : update the current user information.
     *
     * @param user the current user information
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) or 500 (Internal Server Error) if
     * the user couldn't be updated
     */
    @PostMapping("/account")
    @Timed
    @PreAuthorize("hasPermission({'user': #user}, 'ACCOUNT.UPDATE')")
    public ResponseEntity<UserDTO> saveAccount(@Valid @RequestBody UserDTO user) {
        user.getLogins().forEach(userLogin -> userLoginRepository.findOneByLoginIgnoreCaseAndUserIdNot(
            userLogin.getLogin(), user.getId()).ifPresent(s -> {
            throw new BusinessException(LOGIN_IS_USED_ERROR_TEXT);
        }));
        Optional<UserDTO> updatedUser = accountService.updateAccount(user);

        updatedUser.ifPresent(userDTO -> produceEvent(userDTO, Constants.UPDATE_PROFILE_EVENT_TYPE));
        return ResponseUtil.wrapOrNotFound(updatedUser,
                                           HeaderUtil.createAlert("userManagement.updated", user.getUserKey()));
    }


    /**
     * PUT /account/logins : Updates an existing Account logins.
     *
     * @param user the user to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated user, or with status 400 (Bad Request)
     * if the login or email is already in use, or with status 500 (Internal Server Error) if the user couldn't be
     * updated
     */
    @PutMapping("/account/logins")
    @Timed
    @PreAuthorize("hasPermission({'id': #user.userKey, 'newUser': #user}, 'user', 'ACCOUNT.LOGIN.UPDATE')")
    public ResponseEntity<UserDTO> updateUserLogins(@Valid @RequestBody UserDTO user) {
        user.getLogins().forEach(
            userLogin -> userLoginRepository.findOneByLoginIgnoreCaseAndUserIdNot(
                userLogin.getLogin(), user.getId()).ifPresent(s -> {
                throw new BusinessException(LOGIN_IS_USED_ERROR_TEXT);
            }));

        Optional<UserDTO> updatedUser = userService.updateUserLogins(getRequiredUserKey(), user.getLogins());
        updatedUser.ifPresent(userDTO -> produceEvent(userDTO, Constants.UPDATE_PROFILE_EVENT_TYPE));
        return ResponseUtil.wrapOrNotFound(updatedUser,
                                           HeaderUtil.createAlert("userManagement.updated", user.getUserKey()));
    }

    /**
     * POST /account/change_password : changes the current user's password.
     *
     * @param password the new password
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) if the new password is not strong
     * enough
     */
    @PostMapping(path = "/account/change_password", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'password': #password}, 'ACCOUNT.PASSWORD.UPDATE')")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordVM password) {
        UserDTO userDTO = accountService.changePassword(password);
        produceEvent(userDTO, Constants.CHANGE_PASSWORD_EVENT_TYPE);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account/reset_password/init : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user
     * @return the ResponseEntity with status 200 (OK) if the email was sent, or status 400 (Bad Request) if the email
     * address is not registered
     */
    @PostMapping(path = "/account/reset_password/init", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'mail': #mail}, 'ACCOUNT.PASSWORD.RESET')")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody String mail) {
        userService.requestPasswordReset(mail)
            .ifPresent(accountMailService::sendMailOnPasswordInit);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account/reset_password/check : check if key is valid.
     *
     * @param key to check
     * @return the ResponseEntity with status 200 (OK) if key is OK, or status 400 (Bad Request) if key is not valid
     */
    @GetMapping(path = "/account/reset_password/check", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    @PreAuthorize("hasPermission(null, 'ACCOUNT.PASSWORD.RESET.CHECK')")
    public ResponseEntity<Void> checkPasswordReset(@RequestParam("key") String key) {
        userService.checkPasswordReset(key);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account/reset_password/finish : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password
     * @return the ResponseEntity with status 200 (OK) if the password has been reset, or status 400 (Bad Request) or
     * 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset_password/finish", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'keyAndPassword': #keyAndPassword}, 'ACCOUNT.PASSWORD.RESET.FINISH')")
    public ResponseEntity<Void> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new BusinessException(CHECK_ERROR_MESSAGE);
        }
        User user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
        accountMailService.sendMailOnPasswordResetFinish(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password)
            && password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH
            && password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }

    private void produceEvent(UserDTO userDto, String eventType) {
        String content = profileEventProducer.createEventJson(userDto, eventType);
        profileEventProducer.send(content);
    }

    /**
     * POST /account/tfa_enable : enable TFA for current loged in user.
     *
     * @param request TFE enable request
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping(path = "/account/tfa_enable")
    @Timed
    @PreAuthorize("hasPermission(null, 'ACCOUNT.TFA.ENABLE')")
    public ResponseEntity<Void> enableTwoFactorAuth(@Valid @NotNull @RequestBody TfaEnableRequest request) {
        accountService.enableTwoFactorAuth(request.getOtpChannelSpec());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /account/tfa_disable : enable TFA for current loged in user.
     *
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping(path = "/account/tfa_disable")
    @Timed
    @PreAuthorize("hasPermission(null, 'ACCOUNT.TFA.DISABLE')")
    public ResponseEntity<Void> disableTwoFactorAuth() {
        accountService.disableTwoFactorAuth();
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/account/tfa_available_otp_channel_specs")
    @Timed
    @PreAuthorize("hasPermission(null, 'ACCOUNT.TFA.AVAILABLE_OTP_CHANNEL_SPECS')")
    public ResponseEntity<Map<OtpChannelType, List<TfaOtpChannelSpec>>> getTfaAvailableOtpChannelSpecs() {
        Map<OtpChannelType, List<TfaOtpChannelSpec>> channelSpecs = accountService.getTfaAvailableOtpChannelSpecs();
        if (CollectionUtils.isEmpty(channelSpecs)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().body(channelSpecs);
    }

}
