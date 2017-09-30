package com.icthh.xm.uaa.social.twitter.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.uaa.social.twitter.api.Twitter;
import com.icthh.xm.uaa.social.twitter.api.UserOperations;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.oauth1.AbstractOAuth1ApiBinding;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Template;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * This is the central class for interacting with Twitter.
 * <p>
 * Most (not all) Twitter operations require OAuth authentication. To perform
 * such operations, {@link TwitterTemplate} must be constructed with the minimal
 * amount of information required to sign requests to Twitter's API with an
 * OAuth <code>Authorization</code> header.
 * </p>
 *
 * @author Craig Walls
 */
public class TwitterTemplate extends AbstractOAuth1ApiBinding implements Twitter {

    private UserOperations userOperations;

    private RestTemplate clientRestTemplate = null;

    /**
     * Create a new instance of TwitterTemplate.
     *
     * @param consumerKey the application's API key
     * @param consumerSecret the application's API secret
     * @param accessToken an access token acquired through OAuth authentication with Twitter
     * @param accessTokenSecret an access token secret acquired through OAuth authentication with Twitter
     */
    public TwitterTemplate(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        super(consumerKey, consumerSecret, accessToken, accessTokenSecret);
        initSubApis();
    }

    /**
     * Create a new instance of TwitterTemplate. This instance of TwitterTemplate is limited to only performing
     * operations requiring client authorization. For instance, you can use it to search Twitter, but you cannot use it
     * to post a status update. The access token you use here must be obtained via OAuth 2 Client Credentials Grant. See
     * {@link OAuth2Operations#authenticateClient()}.
     *
     * @param clientToken an access token obtained through OAuth 2 client credentials grant with Twitter.
     */
    public TwitterTemplate(String clientToken) {
        super();
        Assert.notNull(clientToken, "Constructor argument 'clientToken' cannot be null.");
        this.clientRestTemplate = createClientRestTemplate(clientToken);
        initSubApis();
    }

    /**
     * Create a new instance of TwitterTemplate. This instance of TwitterTemplate is limited to only performing
     * operations requiring client authorization. For instance, you can use it to search Twitter, but you cannot use it
     * to post a status update. The client credentials given here are used to obtain a client access token via OAuth 2
     * Client Credentials Grant. See {@link OAuth2Operations#authenticateClient()}.
     *
     * @param consumerKey the application's API key
     * @param consumerSecret the application's API secret
     */
    public TwitterTemplate(String consumerKey, String consumerSecret) {
        this(exchangeCredentialsForClientToken(consumerKey, consumerSecret));
    }

    @Override
    public UserOperations userOperations() {
        return userOperations;
    }

    // Override getRestTemplate() to return an app-authorized RestTemplate if a client token is available.
    @Override
    public RestTemplate getRestTemplate() {
        if (clientRestTemplate != null) {
            return clientRestTemplate;
        }
        return super.getRestTemplate();
    }

    // AbstractOAuth1ApiBinding hooks

    @Override
    protected MappingJackson2HttpMessageConverter getJsonMessageConverter() {
        MappingJackson2HttpMessageConverter converter = super.getJsonMessageConverter();
        converter.setObjectMapper(new ObjectMapper().registerModule(new TwitterModule()));
        return converter;
    }

    @Override
    protected FormHttpMessageConverter getFormMessageConverter() {
        return new TwitterEscapingFormHttpMessageConverter();
    }

    @Override
    protected void configureRestTemplate(RestTemplate restTemplate) {
        restTemplate.setErrorHandler(new TwitterErrorHandler());
    }

    // private helper
    private static String exchangeCredentialsForClientToken(String consumerKey, String consumerSecret) {
        OAuth2Template oauth2 = new OAuth2Template(consumerKey, consumerSecret, "",
            "https://api.twitter.com/oauth2/token");
        return oauth2.authenticateClient().getAccessToken();
    }

    private RestTemplate createClientRestTemplate(String clientToken) {
        RestTemplate restTemplate = new ClientAuthorizedTwitterTemplate(clientToken).getRestTemplate();
        restTemplate.setMessageConverters(getMessageConverters());
        configureRestTemplate(restTemplate);
        return restTemplate;
    }

    private void initSubApis() {
        this.userOperations = new UserTemplate(getRestTemplate(), isAuthorized(), isAuthorizedForApp());
    }

    private boolean isAuthorizedForApp() {
        return clientRestTemplate != null;
    }

}
