package com.icthh.xm.uaa.social.twitter.connect;

import com.icthh.xm.uaa.social.twitter.api.Twitter;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;

/**
 * Twitter ConnectionFactory implementation.
 *
 * @author Keith Donald
 */
public class TwitterConnectionFactory extends OAuth1ConnectionFactory<Twitter> {

    public TwitterConnectionFactory(String consumerKey, String consumerSecret) {
        super("twitter", new TwitterServiceProvider(consumerKey, consumerSecret), new TwitterAdapter());
    }

}
