package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.Authority;
import com.icthh.xm.uaa.domain.RegistrationLog;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.AuthorityRepository;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.security.AuthoritiesConstants;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.web.rest.vm.ChangePasswordVM;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class AccountService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityRepository authorityRepository;
    private final RegistrationLogRepository registrationLogRepository;

    /**
     * Register new user.
     *
     * @param user user dto
     * @return new user
     */
    @Transactional
    public User register(ManagedUserVM user, String ipAddress) {

        User newUser = new User();
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setImageUrl(user.getImageUrl());
        newUser.setLangKey(user.getLangKey());
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.USER);
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        newUser.setLogins(user.getLogins());
        newUser.getLogins().forEach(userLogin -> userLogin.setUser(newUser));
        newUser.setData(user.getData());
        userRepository.save(newUser);

        registrationLogRepository.save(new RegistrationLog(ipAddress));

        return newUser;
    }

    /**
     * Activate user by key.
     *
     * @param key activation key
     * @return user
     */
    public Optional<User> activateRegistration(String key) {
        return userRepository.findOneByActivationKey(key)
            .map(user -> {
                user.setActivated(true);
                user.setActivationKey(null);
                return user;
            });
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     */
    public Optional<UserDTO> updateAccount(UserDTO updatedUser) {
        return userRepository.findOneWithLoginsByUserKey(TenantContext.getCurrent().getUserKey()).map(user -> {
            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setLangKey(updatedUser.getLangKey());
            user.setImageUrl(updatedUser.getImageUrl());
            user.setAccessTokenValiditySeconds(updatedUser.getAccessTokenValiditySeconds());
            user.setRefreshTokenValiditySeconds(updatedUser.getRefreshTokenValiditySeconds());
            user.setData(updatedUser.getData());
            return user;
        }).map(UserDTO::new);
    }

    /**
     * Change account password.
     *
     * @param password password
     */
    public void changePassword(ChangePasswordVM password) {
        userRepository.findOneByUserKey(TenantContext.getCurrent().getUserKey()).ifPresent(
            user -> {
                if (!passwordEncoder.matches(password.getOldPassword(), user.getPassword())) {
                    throw new BusinessException("Passed invalid old password");
                }
                String newPassword = passwordEncoder.encode(password.getNewPassword());
                user.setPassword(newPassword);
                userRepository.saveAndFlush(user);
                log.debug("Changed password for User: {}", user);
            });
    }
}
