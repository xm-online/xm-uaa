package com.icthh.xm.uaa.social.twitter.api.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mixin class for adding Jackson annotations to TwitterProfile.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class TwitterProfileMixin extends TwitterObjectMixin {

    @JsonCreator
    TwitterProfileMixin(
        @JsonProperty("id") long id,
        @JsonProperty("email") String email,
        @JsonProperty("screen_name") String screenName,
        @JsonProperty("name") String name,
        @JsonProperty("profile_image_url") String profileImageUrl) {
    }

}
