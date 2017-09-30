package com.icthh.xm.uaa.social.twitter.api.impl;

import com.icthh.xm.uaa.social.twitter.api.TwitterProfile;
import com.icthh.xm.uaa.social.twitter.api.UserOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the {@link UserOperations} interface providing binding to Twitters' user-oriented REST resources.
 *
 * @author Craig Walls
 */
class UserTemplate extends AbstractTwitterOperations implements UserOperations {

    private final RestTemplate restTemplate;

    public UserTemplate(RestTemplate restTemplate, boolean isAuthorizedForUser, boolean isAuthorizedForApp) {
        super(isAuthorizedForUser, isAuthorizedForApp);
        this.restTemplate = restTemplate;
    }

    @Override
    public long getProfileId() {
        requireUserAuthorization();
        return getUserProfile().getId();
    }

    @Override
    public String getScreenName() {
        requireUserAuthorization();
        return getUserProfile().getScreenName();
    }

    @Override
    public TwitterProfile getUserProfile() {
        requireUserAuthorization();
        return restTemplate.getForObject(buildUri("account/verify_credentials.json", "include_email", "true"), TwitterProfile.class);
    }

    @Override
    public TwitterProfile getUserProfile(String screenName) {
        requireEitherUserOrAppAuthorization();
        return restTemplate.getForObject(buildUri("users/show.json", "screen_name", screenName), TwitterProfile.class);
    }

    @Override
    public TwitterProfile getUserProfile(long userId) {
        requireEitherUserOrAppAuthorization();
        return restTemplate
            .getForObject(buildUri("users/show.json", "user_id", String.valueOf(userId)), TwitterProfile.class);
    }

}
