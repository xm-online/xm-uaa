package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.uaa.config.Constants.LOGIN_IS_USED_ERROR_TEXT;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantUtil;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.service.AccountService;
import com.icthh.xm.uaa.service.CaptchaService;
import com.icthh.xm.uaa.service.MailService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.rest.util.HeaderUtil;
import com.icthh.xm.uaa.web.rest.vm.CaptchaVM;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.KeyAndPasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import io.github.jhipster.web.util.ResponseUtil;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final MailService mailService;
    private final ProfileEventProducer profileEventProducer;
    private final CaptchaService captchaService;

    /**
     * POST /register : register the user.
     *
     * @param user the managed user View Model
     * @return the ResponseEntity with status 201 (Created) if the user is registered or 400 (Bad
     *         Request) if the login is already in use
     */
    @PostMapping(path = "/register",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Timed
    public ResponseEntity registerAccount(@Valid @RequestBody ManagedUserVM user, HttpServletRequest request) {
        if (user.getEmail() == null) {
            throw new BusinessException("Email can't be empty");
        }
        user.getLogins().forEach(
            userLogin -> userLoginRepository.findOneByLoginIgnoreCase(userLogin.getLogin()).ifPresent(s -> {
                throw new BusinessException(LOGIN_IS_USED_ERROR_TEXT);
            }));
        if (captchaService.isCaptchaNeed(request.getRemoteAddr())) {
            captchaService.checkCaptcha(user.getCaptcha());
        }
        User newUser = accountService.register(user, request.getRemoteAddr());
        produceEvent(new UserDTO(newUser), Constants.CREATE_PROFILE_EVENT_TYPE);
        mailService.sendActivationEmail(
            newUser, TenantContext.getCurrent().getTenant(), TenantUtil.getApplicationUrl(), MDCUtil.getRid());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/is-captcha-need")
    @Timed
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
     * @return the ResponseEntity with status 200 (OK) and the activated user in body, or status 500
     *         (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    public ResponseEntity<String> activateAccount(@RequestParam("key") String key) {
        return accountService.activateRegistration(key)
            .map(user -> new ResponseEntity<String>(HttpStatus.OK))
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
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET /account : get the current user.
     *
     * @return the ResponseEntity with status 200 (OK) and the current user in body, or status 500
     *         (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    public ResponseEntity<UserDTO> getAccount() {
        return userService.findOneWithAuthoritiesAndLoginsByUserKey(TenantContext.getCurrent().getUserKey())
            .map(user -> new ResponseEntity<>(new UserDTO(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * POST /account : update the current user information.
     *
     * @param user the current user information
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) or 500 (Internal
     *         Server Error) if the user couldn't be updated
     */
    @PostMapping("/account")
    @Timed
    public ResponseEntity saveAccount(@Valid @RequestBody UserDTO user) {
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
     * @return the ResponseEntity with status 200 (OK) and with body the updated user, or with
     *         status 400 (Bad Request) if the login or email is already in use, or with status 500
     *         (Internal Server Error) if the user couldn't be updated
     */
    @PutMapping("/account/logins")
    @Timed
    public ResponseEntity<UserDTO> updateUserLogins(@Valid @RequestBody UserDTO user) {
        user.getLogins().forEach(
            userLogin -> userLoginRepository.findOneByLoginIgnoreCaseAndUserIdNot(
                userLogin.getLogin(), user.getId()).ifPresent(s -> {
                    throw new BusinessException(LOGIN_IS_USED_ERROR_TEXT);
                }));
        Optional<UserDTO> updatedUser = userService.updateUserLogins(TenantContext.getCurrent().getUserKey(),
            user.getLogins());
        updatedUser.ifPresent(userDTO -> produceEvent(userDTO, Constants.UPDATE_PROFILE_EVENT_TYPE));
        return ResponseUtil.wrapOrNotFound(updatedUser,
            HeaderUtil.createAlert("userManagement.updated", user.getUserKey()));
    }

    /**
     * POST /account/change_password : changes the current user's password.
     *
     * @param password the new password
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) if the new
     *         password is not strong enough
     */
    @PostMapping(path = "/account/change_password",
        produces = MediaType.TEXT_PLAIN_VALUE)
    @Timed
    public ResponseEntity changePassword(@Valid @RequestBody ChangePasswordVM password) {
        accountService.changePassword(password);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /account/reset_password/init : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user
     * @return the ResponseEntity with status 200 (OK) if the email was sent, or status 400 (Bad
     *         Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset_password/init",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    public ResponseEntity requestPasswordReset(@RequestBody String mail) {
        return userService.requestPasswordReset(mail)
            .map(user -> {
                String rid = MDCUtil.getRid();
                mailService.sendPasswordResetMail(user, TenantUtil.getApplicationUrl(),
                    TenantContext.getCurrent().getTenant(), rid);
                return new ResponseEntity<>("email was sent", HttpStatus.OK);
            }).orElseThrow(() -> new BusinessException("email address not registered"));
    }

    /**
     * POST /account/reset_password/finish : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password
     * @return the ResponseEntity with status 200 (OK) if the password has been reset, or status 400
     *         (Bad Request) or 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset_password/finish",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Timed
    public ResponseEntity<String> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new BusinessException(CHECK_ERROR_MESSAGE);
        }
        return userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
            .map(user -> new ResponseEntity<String>(HttpStatus.OK))
            .orElseThrow(() -> new BusinessException("User not found with reset key: " + keyAndPassword.getKey()));
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
}
