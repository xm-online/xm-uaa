package com.icthh.xm.uaa.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.domain.RegistrationLog;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.repository.RegistrationLogRepository;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.dto.UserPassDto;
import com.icthh.xm.uaa.service.util.RandomUtil;
import com.icthh.xm.uaa.web.rest.vm.ManagedUserVM;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@LepService(group = "service.account")
public class UserRegistrationComponent {

    private final RegistrationLogRepository registrationLogRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantPropertiesService tenantPropertiesService;
    private final UserService userService;



    /**
     * Register new user.
     *
     * @param user user dto
     * @return new user
     */
    @Transactional
    @LogicExtensionPoint("Register")
    public User register(UserDTO user, String ipAddress) {
        UserPassDto userPassDto = getOrGeneratePassword(user);

        User newUser = new User();
        newUser.setUserKey(UUID.randomUUID().toString());
        newUser.setPassword(userPassDto.getEncryptedPassword());
        newUser.setPasswordSetByUser(userPassDto.getSetByUser());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setImageUrl(user.getImageUrl());
        newUser.setLangKey(user.getLangKey());
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        newUser.setRoleKey(tenantPropertiesService.getTenantProps().getSecurity().getDefaultUserRole());
        newUser.setLogins(user.getLogins());
        newUser.getLogins().forEach(userLogin -> userLogin.setUser(newUser));
        newUser.setData(user.getData());

        User resultUser = userRepository.save(newUser);

        registrationLogRepository.save(new RegistrationLog(ipAddress));

        return resultUser;
    }

    private UserPassDto getOrGeneratePassword(UserDTO user) {
        if (user instanceof ManagedUserVM) {
            ManagedUserVM managedUser = (ManagedUserVM) user;
            userService.validatePassword(managedUser.getPassword());
            return new UserPassDto(passwordEncoder.encode(managedUser.getPassword()), true);
        }
        return new UserPassDto(passwordEncoder.encode(UUID.randomUUID().toString()), false);
    }
}
