package com.icthh.xm.uaa.security;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@LepService(group = "security.validator")
public class UserSecurityValidator {

    private final UserService userService;

    @LogicExtensionPoint("IsUserActivated")
    public boolean isUserActivated(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String)) {
            throw new InvalidTokenException("Invalid principal");
        }
        String userLogin = (String) principal;
        return userService.findOneByLogin(userLogin).map(User::isActivated).orElse(false);
    }
}
