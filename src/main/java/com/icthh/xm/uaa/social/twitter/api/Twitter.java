package com.icthh.xm.uaa.social.twitter.api;

import org.springframework.social.ApiBinding;

/**
 * Interface specifying a basic set of operations for interacting with Twitter.
 * Implemented by TwitterTemplate.
 */
public interface Twitter extends ApiBinding {

    /**
     * @return the portion of the Twitter API containing the user operations.
     */
    UserOperations userOperations();

}
