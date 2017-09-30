package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.commons.logging.util.MDCUtil;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.config.tenant.TenantUtil;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.kafka.ProfileEventProducer;
import com.icthh.xm.uaa.security.AuthoritiesConstants;
import com.icthh.xm.uaa.service.MailService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.web.rest.util.HeaderUtil;
import com.icthh.xm.uaa.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import io.swagger.annotations.ApiParam;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST controller for managing users.
 *
 * <p>This class accesses the User entity, and needs to fetch its collection of authorities.
 *
 * <p>For a normal use-case, it would be better to have an eager relationship between User and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 *
 * <p>We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will
 * quite often do relationships with the user, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our users'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 *
 * <p>Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api")
@Slf4j
@AllArgsConstructor
public class UserResource {

    private static final String ENTITY_NAME = "userManagement";

    private final UserLoginRepository userLoginRepository;

    private final MailService mailService;

    private final UserService userService;

    private final ProfileEventProducer profileEventProducer;

    /**
     * POST /users : Creates a new user.
     *
     * <p>Creates a new user if the login and email are not already used, and sends an mail with an
     * activation link. The user needs to be activated on creation.
     * @param user the user to create
     * @return the ResponseEntity with status 201 (Created) and with body the new user, or with
     *         status 400 (Bad Request) if the login or email is already in use
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/users")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity createUser(@Valid @RequestBody UserDTO user) throws URISyntaxException {
        if (user.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new user cannot already have an ID"))
                .body(null);
        }
        user.getLogins().forEach(userLogin ->
            userLoginRepository.findOneByLoginIgnoreCase(userLogin.getLogin())
                .ifPresent(s -> {
                    throw new BusinessException(Constants.LOGIN_IS_USED_ERROR_TEXT);
                })
        );
        User newUser = userService.createUser(user);
        produceEvent(new UserDTO(newUser), Constants.CREATE_PROFILE_EVENT_TYPE);
        mailService.sendCreationEmail(newUser, TenantUtil.getApplicationUrl(),
            TenantContext.getCurrent().getTenant(), MDCUtil.getRid());
        return ResponseEntity.created(new URI("/api/users/" + newUser.getUserKey()))
            .headers(HeaderUtil.createAlert("userManagement.created", newUser.getUserKey()))
            .body(new UserDTO(newUser));
    }

    /**
     * PUT  /users : Updates an existing User.
     *
     * @param user the user to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated user
     */
    @PutMapping("/users")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UserDTO user) {
        Optional<UserDTO> updatedUser = userService.updateUser(user);
        updatedUser.ifPresent(userDTO -> produceEvent(userDTO, Constants.UPDATE_PROFILE_EVENT_TYPE));
        return ResponseUtil.wrapOrNotFound(updatedUser,
            HeaderUtil.createAlert("userManagement.updated", user.getUserKey()));
    }


    /**
     * PUT /user/logins : Updates an existing User logins.
     * @param user the user to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated user, or with
     *         status 400 (Bad Request) if the login or email is already in use, or with status 500
     *         (Internal Server Error) if the user couldn't be updated
     */
    @PutMapping("/users/logins")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<UserDTO> updateUserLogins(@Valid @RequestBody UserDTO user) {
        user.getLogins().forEach(userLogin ->
            userLoginRepository.findOneByLoginIgnoreCaseAndUserIdNot(userLogin.getLogin(), user.getId())
                .ifPresent(s -> {
                    throw new BusinessException(Constants.LOGIN_IS_USED_ERROR_TEXT);
                })
        );
        Optional<UserDTO> updatedUser = userService.updateUserLogins(user.getUserKey(), user.getLogins());
        updatedUser.ifPresent(userDTO -> produceEvent(userDTO, Constants.UPDATE_PROFILE_EVENT_TYPE));
        return ResponseUtil.wrapOrNotFound(updatedUser,
            HeaderUtil.createAlert("userManagement.updated", user.getUserKey()));
    }

    /**
     * GET  /users : get all users.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and with body all users
     */
    @GetMapping("/users")
    @Timed
    public ResponseEntity<List<UserDTO>> getAllUsers(@ApiParam Pageable pageable) {
        final Page<UserDTO> page = userService.getAllManagedUsers(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/users");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * Get list of all roles.
     *
     * @return a string list of the all of the roles
     */
    @GetMapping("/users/authorities")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAuthorities() {
        return userService.getAuthorities();
    }

    /**
     * GET  /users/:userKey : get the "userKey" user.
     *
     * @param userKey the userKey of the user to find
     * @return the ResponseEntity with status 200 (OK) and with body the "userKey" user, or with status 404 (Not Found)
     */
    @GetMapping("/users/{userKey}")
    @Timed
    public ResponseEntity<UserDTO> getUser(@PathVariable String userKey) {
        return ResponseUtil.wrapOrNotFound(
            userService.findOneWithAuthoritiesAndLoginsByUserKey(userKey)
                .map(UserDTO::new));
    }

    /**
     * DELETE /users/:userKey : delete the "userKey" User.
     *
     * @param userKey the user key of the user to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/users/{userKey}")
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteUser(@PathVariable String userKey) {
        userService.deleteUser(userKey);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert("userManagement.deleted", userKey)).build();
    }

    private void produceEvent(UserDTO userDto, String eventType) {
        String content = profileEventProducer.createEventJson(userDto, eventType);
        profileEventProducer.send(content);
    }
}
