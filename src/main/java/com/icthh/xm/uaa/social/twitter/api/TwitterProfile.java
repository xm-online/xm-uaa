package com.icthh.xm.uaa.social.twitter.api;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.social.twitter.api.TwitterObject;

/**
 * Model class representing a Twitter user's profile information.
 */
public class TwitterProfile extends TwitterObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String email;
    private final String screenName;
    private final String name;
    private final String profileImageUrl;

    public TwitterProfile(long id, String email, String screenName, String name, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.screenName = screenName;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getProfileUrl() {
        return "http://twitter.com/" + screenName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TwitterProfile that = (TwitterProfile) o;

        return new EqualsBuilder()
            .append(id, that.id)
            .append(name, that.name)
            .append(screenName, that.screenName)
            .append(email, that.email)
            .append(profileImageUrl, that.profileImageUrl)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(id)
            .append(name)
            .append(screenName)
            .append(email)
            .append(profileImageUrl)
            .hashCode();
    }

}
