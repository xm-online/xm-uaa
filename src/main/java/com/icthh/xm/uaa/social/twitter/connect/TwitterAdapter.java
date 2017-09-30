package com.icthh.xm.uaa.social.twitter.connect;

import com.icthh.xm.uaa.social.twitter.api.Twitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.social.ApiException;
import org.springframework.social.connect.ApiAdapter;
import org.springframework.social.connect.ConnectionValues;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UserProfileBuilder;
import com.icthh.xm.uaa.social.twitter.api.TwitterProfile;

/**
 * Twitter ApiAdapter implementation.
 */
@Slf4j
public class TwitterAdapter implements ApiAdapter<Twitter> {

    @Override
    public boolean test(Twitter twitter) {
        try {
            twitter.userOperations().getUserProfile();
            return true;
        } catch (ApiException e) {
            log.error("Error during twitter API test", e);
            return false;
        }
    }

    @Override
    public void setConnectionValues(Twitter twitter, ConnectionValues values) {
        TwitterProfile profile = twitter.userOperations().getUserProfile();
        values.setProviderUserId(Long.toString(profile.getId()));
        values.setDisplayName("@" + profile.getScreenName());
        values.setProfileUrl(profile.getProfileUrl());
        values.setImageUrl(profile.getProfileImageUrl());
    }

    @Override
    public UserProfile fetchUserProfile(Twitter twitter) {
        TwitterProfile profile = twitter.userOperations().getUserProfile();
        return new UserProfileBuilder().setName(profile.getName()).setUsername(profile.getScreenName())
            .setEmail(profile.getEmail()).build();
    }

    @Override
    public void updateStatus(Twitter twitter, String message) {

    }

}
