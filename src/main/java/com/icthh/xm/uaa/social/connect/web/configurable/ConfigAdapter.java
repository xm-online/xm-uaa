package com.icthh.xm.uaa.social.connect.web.configurable;

import org.springframework.social.connect.ApiAdapter;
import org.springframework.social.connect.ConnectionValues;
import org.springframework.social.connect.UserProfile;

public class ConfigAdapter implements ApiAdapter<ConfigOAuth2Api> {

	public boolean test(ConfigOAuth2Api api) {
        return api.test();
	}

	public void setConnectionValues(ConfigOAuth2Api api, ConnectionValues values) {
        // TODO
	}

	public UserProfile fetchUserProfile(ConfigOAuth2Api api) {
        // TODO
        return null;
	}

	public void updateStatus(ConfigOAuth2Api api, String message) {
		throw new UnsupportedOperationException();
	}

}
