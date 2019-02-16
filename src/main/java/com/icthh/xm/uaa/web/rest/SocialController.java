package com.icthh.xm.uaa.web.rest;

import com.icthh.xm.uaa.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
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
        return null;// new RedirectView(socialService.initSocialLogin(providerId), false);
    }

    @GetMapping(value = "/signin/{providerId}", params = "code")
    public RedirectView oauth2Callback(@PathVariable String providerId, @RequestParam("code") String code,
                                       ServletWebRequest request) {
        //String token = socialService.loginUser(providerId, code);
        //request.getResponse().addHeader(HttpHeaders.AUTHORIZATION, token);
        return null;
    }

}
