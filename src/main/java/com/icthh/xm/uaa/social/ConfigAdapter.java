package com.icthh.xm.uaa.social;

import org.springframework.social.connect.ApiAdapter;
import org.springframework.social.connect.ConnectionValues;
import org.springframework.social.connect.UserProfile;

public class ConfigAdapter implements ApiAdapter<ConfigOAuth2Api> {

    public boolean test(ConfigOAuth2Api api) {
        throw new UnsupportedOperationException();
    }

    public void setConnectionValues(ConfigOAuth2Api api, ConnectionValues values) {
        ConnectionValuesDto connectionValues = api.fetchConnectionValues();
        values.setDisplayName(connectionValues.getDisplayName());
        values.setImageUrl(connectionValues.getImageUrl());
        values.setProfileUrl(connectionValues.getProfileUrl());
        values.setProviderUserId(connectionValues.getProviderUserId());
    }

    public UserProfile fetchUserProfile(ConfigOAuth2Api api) {
        return api.fetchUserProfile();
    }

    public void updateStatus(ConfigOAuth2Api api, String message) {
        throw new UnsupportedOperationException();
    }

}
