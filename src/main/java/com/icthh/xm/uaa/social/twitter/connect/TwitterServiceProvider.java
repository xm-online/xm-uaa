package com.icthh.xm.uaa.social.twitter.connect;

import com.icthh.xm.uaa.social.twitter.api.Twitter;
import com.icthh.xm.uaa.social.twitter.api.impl.TwitterTemplate;
import org.springframework.social.oauth1.AbstractOAuth1ServiceProvider;
import org.springframework.social.oauth1.OAuth1Template;

/**
 * Twitter ServiceProvider implementation.
 */
public class TwitterServiceProvider extends AbstractOAuth1ServiceProvider<Twitter> {

    public TwitterServiceProvider(String consumerKey, String consumerSecret) {
        super(consumerKey, consumerSecret, new OAuth1Template(consumerKey, consumerSecret,
            "https://api.twitter.com/oauth/request_token",
            "https://api.twitter.com/oauth/authorize",
            "https://api.twitter.com/oauth/authenticate",
            "https://api.twitter.com/oauth/access_token"));
    }

    @Override
    public Twitter getApi(String accessToken, String secret) {
        return new TwitterTemplate(getConsumerKey(), getConsumerSecret(), accessToken, secret);
    }

}
