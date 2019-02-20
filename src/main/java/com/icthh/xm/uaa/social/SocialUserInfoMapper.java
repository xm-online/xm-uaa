package com.icthh.xm.uaa.social;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.domain.properties.TenantProperties.UserInfoMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.connect.UserProfile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
@LepService(group = "service.social")
public class SocialUserInfoMapper {

    @LogicExtensionPoint("MapToSocialUserInfo")
    public SocialUserInfo toSocialUserInfo(UserInfoMapping mapping, Map<String, Object> userInfo) {
        return new SocialUserInfo(
            map(mapping::getId, userInfo),
            map(mapping::getName, userInfo),
            map(mapping::getFirstName, userInfo),
            map(mapping::getLastName, userInfo),
            map(() -> defaultIfBlank(mapping.getEmail(), "email"), userInfo),
            map(mapping::getUsername, userInfo),
            map(mapping::getProfileUrl, userInfo),
            map(mapping::getImageUrl, userInfo),
            map(mapping::getPhoneNumber, userInfo),
            map(mapping::getLangKey, userInfo),
            map(mapping::getEmailVerifiedCheckField, userInfo)
        );
    }

    @LogicExtensionPoint("MapToUserProfile")
    public UserProfile toUserProfile(UserInfoMapping mapping, Map<String, Object> userInfo) {
        return new UserProfile(
            map(mapping::getId, userInfo),
            map(mapping::getName, userInfo),
            map(mapping::getFirstName, userInfo),
            map(mapping::getLastName, userInfo),
            map(() -> defaultIfBlank(mapping.getEmail(), "email"), userInfo),
            map(mapping::getUsername, userInfo)
        );
    }

    @LogicExtensionPoint("MapToConnectionValues")
    public ConnectionValuesDto toConnectionValues(UserInfoMapping mapping, Map<String, Object> userInfo) {
        return new ConnectionValuesDto(
            map(mapping::getId, userInfo),
            map(mapping::getName, userInfo),
            map(mapping::getProfileUrl, userInfo),
            map(mapping::getImageUrl, userInfo)
        );
    }

    private String map(Supplier<String> mapping, Map<String, Object> userInfo) {
        if (isBlank(mapping.get())) {
            return null;
        }
        Object value = userInfo;
        for(String property: mapping.get().split("\\.")) {
            value = getProperty(Optional.ofNullable(value), property);
        }
        return value == null ? null : String.valueOf(value);
    }

    private Object getProperty(Optional<Object> object, String property) {
        return object.filter(Map.class::isInstance)
            .map (Map.class::cast)
            .map(map -> map.get(property))
            .orElse(null);
    }

}
