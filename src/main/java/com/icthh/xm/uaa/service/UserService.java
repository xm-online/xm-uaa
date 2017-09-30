package com.icthh.xm.uaa.service;

import com.icthh.xm.uaa.domain.Authority;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLoginType;
import com.icthh.xm.uaa.repository.AuthorityRepository;
import com.icthh.xm.uaa.repository.UserLoginRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.util.RandomUtil;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    private final UserLoginRepository userLoginRepository;

    private final PasswordEncoder passwordEncoder;

    private final SocialService socialService;

    private final AuthorityRepository authorityRepository;

    /**
     * Search user by reset key and set him password.
     *
     * @param newPassword new user password
     * @param key user reset key
     * @return user with new password
     */
    public Optional<User> completePasswordReset(String newPassword, String key) {
        return userRepository.findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                return user;
            });
    }

    /**
     * Search user by mail login and set him reset key.
     *
     * @param mail users mail login
     * @return user
     */
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
    public User createUser(UserDTO user) {
        User newUser = new User();
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setImageUrl(user.getImageUrl());
        newUser.setLangKey(user.getLangKey() == null ? "en" : user.getLangKey());
        if (user.getAuthorities() != null) {
            Set<Authority> authorities = new HashSet<>();
            user.getAuthorities().forEach(
                authority -> authorities.add(authorityRepository.findOne(authority))
            );
            newUser.setAuthorities(authorities);
        }
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
    public Optional<UserDTO> updateUser(UserDTO updatedUser) {
        return Optional.of(userRepository
            .findOne(updatedUser.getId()))
            .map(user -> {
                user.setFirstName(updatedUser.getFirstName());
                user.setLastName(updatedUser.getLastName());
                user.setImageUrl(updatedUser.getImageUrl());
                user.setActivated(updatedUser.isActivated());
                user.setLangKey(updatedUser.getLangKey());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                updatedUser.getAuthorities().stream()
                    .map(authorityRepository::findOne)
                    .forEach(managedAuthorities::add);
                user.setData(updatedUser.getData());
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
        Optional<User> f =  userRepository
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
        userRepository.findOneWithLoginsByUserKey(userKey).ifPresent(user -> {
            socialService.deleteUserSocialConnection(user.getUserKey());
            userRepository.delete(user);
        });
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::new);
    }

    /**
     * Search user by user key.
     * @param userKey user key
     * @return user
     */
    @Transactional(readOnly = true)
    public Optional<User> findOneWithAuthoritiesAndLoginsByUserKey(String userKey) {
        if (StringUtils.isBlank(userKey)) {
            log.warn("User key is empty");
            return Optional.empty();
        }
        return userRepository.findOneWithAuthoritiesAndLoginsByUserKey(userKey);
    }

    @Transactional(readOnly = true)
    public User getUserWithAuthorities(Long id) {
        return userRepository.findOneWithAuthoritiesById(id);
    }

    /**
     * Get authorities.
     *
     * @return a list of all the authorities
     */
    public List<String> getAuthorities() {
        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public User getUser(String userKey) {
        return userRepository.findOneByUserKey(userKey).orElse(null);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
