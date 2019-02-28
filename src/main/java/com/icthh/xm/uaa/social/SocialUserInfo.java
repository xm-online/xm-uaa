package com.icthh.xm.uaa.social;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SocialUserInfo {
    private final String id;
    private final String name;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String username;
    private final String profileUrl;
    private final String imageUrl;
    private final String phoneNumber;
    private final String langKey;
    private final String emailVerified;

    private Map<String, Object> data = new HashMap<>();
}
