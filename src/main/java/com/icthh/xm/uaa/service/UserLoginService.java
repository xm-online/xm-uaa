package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.uaa.config.Constants.LOGIN_USED_CODE;
import static com.icthh.xm.uaa.config.Constants.LOGIN_USED_PARAM;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Service class for managing user's login.
 */
@LepService(group = "service.user")
@Transactional
@AllArgsConstructor
@Slf4j
public class UserLoginService {

    private final UserLoginRepository userLoginRepository;

    public Optional<UserLogin> getUserByLogin(String login) {
        return userLoginRepository.findOneByLoginIgnoreCase(login);
    }

    @LogicExtensionPoint("NormalizeLogins")
    public void normalizeLogins(List<UserLogin> logins) {
        logins.forEach(userLogin ->
            userLogin.setLogin(
                lowerCase(
                    trim(
                        userLogin.getLogin()
                    )
                )
            )
        );
    }

    /**
     * Verify that logins not exist in the system
     * @param userLogins
     */
    public void verifyLoginsNotExist(List<UserLogin> userLogins) {
        userLogins.forEach(userLogin ->
            userLoginRepository.findOneByLoginIgnoreCase(userLogin.getLogin())
                .ifPresent(s -> {
                    Map<String, String> params = new HashMap<>();
                    params.put(LOGIN_USED_PARAM, s.getTypeKey());
                    throw new BusinessException(LOGIN_USED_CODE, Constants.LOGIN_IS_USED_ERROR_TEXT, params);
                })
        );
    }

    /**
     * Verify that logins not exist in the system except by current owner
     * @param userLogins
     */
    public void verifyLoginsNotExist(List<UserLogin> userLogins, Long userId) {
        userLogins.forEach(userLogin ->
            userLoginRepository.findOneByLoginIgnoreCaseAndUserIdNot(userLogin.getLogin(), userId)
                .ifPresent(s -> {
                    Map<String, String> params = new HashMap<>();
                    params.put(LOGIN_USED_PARAM, s.getTypeKey());
                    throw new BusinessException(LOGIN_USED_CODE, Constants.LOGIN_IS_USED_ERROR_TEXT, params);
                })
        );
    }
}
