/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.icthh.xm.uaa.social.twitter.api;

import org.springframework.social.ApiException;
import org.springframework.social.MissingAuthorizationException;


/**
 * Interface defining the operations for retrieving information about Twitter users.
 *
 * @author Craig Walls
 */
public interface UserOperations {

    /**
     * Retrieves the authenticated user's Twitter ID.
     *
     * @return the user's ID at Twitter
     * @throws ApiException if there is an error while communicating with Twitter.
     * @throws MissingAuthorizationException if TwitterTemplate was not created with OAuth credentials.
     */
    long getProfileId();

    /**
     * Retrieves the authenticated user's Twitter screen name
     *
     * @return the user's screen name
     * @throws ApiException if there is an error while communicating with Twitter.
     * @throws MissingAuthorizationException if TwitterTemplate was not created with OAuth credentials.
     */
    String getScreenName();

    /**
     * Retrieves the authenticated user's Twitter profile details.
     *
     * @return a {@link TwitterProfile} object representing the user's profile.
     * @throws ApiException if there is an error while communicating with Twitter.
     * @throws MissingAuthorizationException if TwitterTemplate was not created with OAuth credentials.
     */
    TwitterProfile getUserProfile();

    /**
     * Retrieves a specific user's Twitter profile details.
     * Supports either user or application authorization.
     *
     * @param screenName the screen name for the user whose details are to be retrieved.
     * @return a {@link TwitterProfile} object representing the user's profile.
     * @throws ApiException if there is an error while communicating with Twitter.
     * @throws MissingAuthorizationException if TwitterTemplate was not created with OAuth credentials or an application
     * access token.
     */
    TwitterProfile getUserProfile(String screenName);

    /**
     * Retrieves a specific user's Twitter profile details.
     * Supports either user or application authorization.
     *
     * @param userId the user ID for the user whose details are to be retrieved.
     * @return a {@link TwitterProfile} object representing the user's profile.
     * @throws ApiException if there is an error while communicating with Twitter.
     * @throws MissingAuthorizationException if TwitterTemplate was not created with OAuth credentials or an application
     * access token.
     */
    TwitterProfile getUserProfile(long userId);

}
