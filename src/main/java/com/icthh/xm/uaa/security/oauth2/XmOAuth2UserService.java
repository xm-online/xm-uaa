package com.icthh.xm.uaa.security.oauth2;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.security.DomainUserDetails;
import com.icthh.xm.uaa.security.DomainUserDetailsService;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static java.util.Optional.ofNullable;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.FAMILY_NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.GIVEN_NAME;

@Component
@RequiredArgsConstructor
@Slf4j
public class XmOAuth2UserService extends DefaultOAuth2UserService {

    private final DomainUserDetailsService userDetailsService;
    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oath2User = super.loadUser(userRequest);
        createUserIfNotRegistered(oath2User, userRequest.getClientRegistration().getClientId());
        return oath2User;
    }

    /**
     * Create the user if not already registered
     * @param oath2User the user from auth server
     * @param clientId the {@link ClientRegistration} identifier used to resolve user mapper
     */
    public void createUserIfNotRegistered(OAuth2User oath2User, String clientId) {
        UserLogin login = getEmailLogin(oath2User);
        Optional<DomainUserDetails> existedUser = userDetailsService.findUserByUsername(login.getLogin());
        if (existedUser.isEmpty()) {
            createNewUser(oath2User, resolveUserMapper(clientId));
        }
    }

    /**
     * Resolve mapper by OAuth client ID
     * @param clientId the OAuth2 provider identifier
     * @return the user mapper
     */
    public Object resolveUserMapper(String clientId) {
        return new Object();//todo implement mapper resolver logic
    }

    private UserLogin getEmailLogin(OAuth2User oath2User) {
        return ofNullable(oath2User.getAttribute(EMAIL))
            .map(Object::toString)
            .map(email -> {
                UserLogin userLogin = new UserLogin();
                userLogin.setLogin(email);
                userLogin.setTypeKey(UserLoginType.EMAIL.getValue());
                return userLogin;
            })
            .orElseThrow(() -> new EntityNotFoundException("Email not found"));
    }

    private void createNewUser(OAuth2User oath2User, Object userMapper) {
        UserDTO userDTO = mapUser(userMapper, oath2User);//todo implement mapper
        userService.createUser(userDTO);
    }

    /**
     * Map from {@link OAuth2User} to {@link UserDTO} object.
     * <p>
     * NOTE: this is dummy implementation
     * </p>
     * @param userMapper the mapper object which will be used
     * @param oath2User the user object used as data source
     * @return the new {@link UserDTO} object
     */
    private UserDTO mapUser(Object userMapper, OAuth2User oath2User) {
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName(oath2User.getAttribute(GIVEN_NAME));
        userDTO.setLastName(oath2User.getAttribute(FAMILY_NAME));
        //NOTE: OAuth2User#getName returns identifier of the end-user
        userDTO.setUserKey(oath2User.getName());//todo allow UserService#createUser save the specified user key
        userDTO.setLogins(Collections.singletonList(getEmailLogin(oath2User)));
        userDTO.setRoleKey(SUPER_ADMIN);
        return userDTO;
    }
}
