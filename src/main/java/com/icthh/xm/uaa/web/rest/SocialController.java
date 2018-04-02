package com.icthh.xm.uaa.web.rest;

import static java.util.stream.Collectors.toList;

import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.domain.SocialConfig;
import com.icthh.xm.uaa.repository.SocialConfigRepository;
import com.icthh.xm.uaa.service.SocialService;
import com.icthh.xm.uaa.social.connect.web.ConnectSupport;
import com.icthh.xm.uaa.social.connect.web.ProviderSignInAttempt;
import com.icthh.xm.uaa.social.connect.web.ProviderSignInUtils;
import com.icthh.xm.uaa.social.connect.web.SessionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.support.OAuth1ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.support.URIBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequestMapping("/social")
public class SocialController {

    private final Logger log = LoggerFactory.getLogger(SocialController.class);

    private static final String POST_SIGN_IN_URL = "/";

    private final SocialService socialService;

    private final ProviderSignInUtils providerSignInUtils;

    private final ConnectionFactoryLocator connectionFactoryLocator;

    private final UsersConnectionRepository usersConnectionRepository;

    private final SignInAdapter signInAdapter;

    private final ConnectSupport connectSupport;

    private final SessionStrategy sessionStrategy;

    private final SocialConfigRepository socialConfigRepository;

    private final XmRequestContextHolder xmRequestContextHolder;

    public SocialController(SocialService socialService,
                            ProviderSignInUtils providerSignInUtils,
                            ConnectionFactoryLocator connectionFactoryLocator,
                            UsersConnectionRepository usersConnectionRepository,
                            SignInAdapter signInAdapter,
                            ConnectSupport connectSupport,
                            SessionStrategy sessionStrategy,
                            SocialConfigRepository socialConfigRepository,
                            XmRequestContextHolder xmRequestContextHolder) {
        this.socialService = socialService;
        this.providerSignInUtils = providerSignInUtils;
        this.connectionFactoryLocator = connectionFactoryLocator;
        this.usersConnectionRepository = usersConnectionRepository;
        this.signInAdapter = signInAdapter;
        this.connectSupport = connectSupport;
        this.sessionStrategy = sessionStrategy;
        this.socialConfigRepository = socialConfigRepository;
        this.xmRequestContextHolder = xmRequestContextHolder;
    }

    @GetMapping("/signup")
    @PreAuthorize("hasPermission(null, 'SOCIAL.SIGN_UP')")
    public RedirectView signUp(WebRequest webRequest,
                               @CookieValue(name = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "\"en\"") String langKey) {
        String providerId = null;
        try {
            Connection<?> connection = providerSignInUtils.getConnectionFromSession(webRequest);
            providerId = connection.getKey().getProviderId();
            socialService.createSocialUser(connection, langKey.replace("\"", ""));
            return redirect(URIBuilder
                                .fromUri(UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/social-register/"
                                             + connection.getKey().getProviderId())
                                .queryParam("success", "true").build().toString());
        } catch (Exception e) {
            log.error("Exception creating social user: ", e);
            return redirectOnError(providerId);
        }
    }

    @PostMapping(value = "/signin/{providerId}")
    @PreAuthorize("hasPermission(null, 'SOCIAL.SIGN_IN')")
    public RedirectView signIn(@PathVariable String providerId, NativeWebRequest request) {
        try {
            ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId);
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            return redirectAbsolute(connectSupport.buildOAuthUrl(connectionFactory, request, parameters));
        } catch (Exception e) {
            log.error("Exception while building authorization URL: ", e);
            return redirectOnError(providerId);
        }
    }

    @GetMapping(value = "/signin/{providerId}", params = "oauth_token")
    public RedirectView oauth1Callback(@PathVariable String providerId, NativeWebRequest request) {
        try {
            OAuth1ConnectionFactory<?> connectionFactory = (OAuth1ConnectionFactory<?>) connectionFactoryLocator
                .getConnectionFactory(providerId);
            Connection<?> connection = connectSupport.completeConnection(connectionFactory, request);
            return handleSignIn(connection, connectionFactory, request);
        } catch (Exception e) {
            log.error("Exception while completing OAuth 1.0(a) connection: ", e);
            return redirectOnError(providerId);
        }
    }

    @GetMapping(value = "/signin/{providerId}", params = "code")
    public RedirectView oauth2Callback(@PathVariable String providerId, @RequestParam("code") String code,
                                       NativeWebRequest request) {
        try {
            OAuth2ConnectionFactory<?> connectionFactory = (OAuth2ConnectionFactory<?>) connectionFactoryLocator
                .getConnectionFactory(providerId);
            Connection<?> connection = connectSupport.completeConnection(connectionFactory, request);
            return handleSignIn(connection, connectionFactory, request);
        } catch (Exception e) {
            log.error("Exception while completing OAuth 2 connection: ", e);
            return redirectOnError(providerId);
        }
    }

    @GetMapping("/providers")
    @PreAuthorize("hasPermission(null, 'SOCIAL.PROVIDERS.GET_LIST')")
    public ResponseEntity<List<String>> providers() {
        String domain = UaaUtils.getRequestDomain(xmRequestContextHolder);
        return ResponseEntity.ok(
            socialConfigRepository.findByDomain(domain)
                .stream()
                .map(SocialConfig::getProviderId)
                .collect(toList())
        );
    }

    private RedirectView handleSignIn(Connection<?> connection, ConnectionFactory<?> connectionFactory,
                                      NativeWebRequest request) {
        List<String> userIds = usersConnectionRepository.findUserIdsWithConnection(connection);
        if (userIds.isEmpty()) {
            ProviderSignInAttempt signInAttempt = new ProviderSignInAttempt(connection);
            sessionStrategy.setAttribute(request, ProviderSignInAttempt.SESSION_ATTRIBUTE, signInAttempt);
            return redirect(getSignUpUrl());
        } else if (userIds.size() == 1) {
            usersConnectionRepository.createConnectionRepository(userIds.get(0)).updateConnection(connection);
            String originalUrl = signInAdapter.signIn(userIds.get(0), connection, request);
            return originalUrl != null ? redirect(originalUrl) : redirect(POST_SIGN_IN_URL);
        } else {
            log.error("Find more than one user with connection key: {}", connection.getKey());
            return redirectOnError(connection.getKey().getProviderId());
        }
    }

    private RedirectView redirect(String url) {
        return new RedirectView(url, true);
    }

    private RedirectView redirectAbsolute(String url) {
        return new RedirectView(url, false);
    }

    private String getSignUpUrl() {
        return UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/uaa/social/signup";
    }

    private RedirectView redirectOnError(String providerId) {
        return redirect(URIBuilder.fromUri(UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/social-register/" + providerId)
                            .queryParam("success", "false").build().toString());
    }

}
