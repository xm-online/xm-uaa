package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.uaa.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final Logger log = LoggerFactory.getLogger(SocialController.class);

    private static final String POST_SIGN_IN_URL = "/";

    private final SocialService socialService;

    @PostMapping(value = "/signin/{providerId}")
    @PreAuthorize("hasPermission(null, 'SOCIAL.SIGN_IN')")
    public RedirectView signIn(@PathVariable String providerId) {
        return new RedirectView(socialService.initSocialLogin(providerId), false);
    }

    @GetMapping(value = "/signin/{providerId}", params = "code")
    public RedirectView oauth2Callback(@PathVariable String providerId, @RequestParam("code") String code,
                                       NativeWebRequest request) {
        return handleSignIn(connection, connectionFactory, request);
    }

}
