package com.icthh.xm.uaa.web.rest;

import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.NEED_ACCEPT_CONNECTION;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.REGISTERED;
import static com.icthh.xm.uaa.social.SocialLoginAnswer.AnswerType.SING_IN;
import static org.springframework.social.support.URIBuilder.fromUri;

import com.icthh.xm.uaa.commons.UaaUtils;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;
import com.icthh.xm.uaa.service.SocialService;
import com.icthh.xm.uaa.social.SocialLoginAnswer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.social.support.URIBuilder;
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
    private final XmRequestContextHolder xmRequestContextHolder;

    @PostMapping(value = "/signin/{providerId}")
    @PreAuthorize("hasPermission(null, 'SOCIAL.SIGN_IN')")
    public RedirectView signIn(@PathVariable String providerId) {
        try {
            return new RedirectView(socialService.initSocialLogin(providerId), false);
        } catch (Exception e) {
            return redirectOnError(providerId);
        }
    }

    @GetMapping(value = "/signin/{providerId}", params = "code")
    public RedirectView oauth2Callback(@PathVariable String providerId, @RequestParam("code") String code,
                                       ServletWebRequest request) {
        try {
            SocialLoginAnswer answer = socialService.acceptSocialLoginUser(providerId, code);
            if (answer.getAnswerType() == SING_IN || answer.getAnswerType() == REGISTERED) {
                OAuth2AccessToken token = answer.getOAuth2AccessToken();
                request.getResponse().addHeader(HttpHeaders.AUTHORIZATION, token.getTokenType() + " " + token.getValue());
                return redirect("/");
            } else if (answer.getAnswerType() == NEED_ACCEPT_CONNECTION) {
                request.getResponse().addHeader("X-ACTIVATION-CODE", answer.getActivationCode());
                return redirect("/social-accept-connection");
            }

            return redirectOnError(providerId);
        } catch (Exception e) {
            return redirectOnError(providerId);
        }
    }

    @PostMapping(value = "/accept/{activationCode}")
    @PreAuthorize("hasPermission(null, 'SOCIAL.ACCEPT_CONNECTION')")
    public void acceptConnection(@PathVariable String activationCode) {
        socialService.acceptConnection(activationCode);
    }

    private RedirectView redirect(String url) {
        return new RedirectView(url, true);
    }

    private RedirectView redirectOnError(String providerId) {
        return redirect(fromUri(UaaUtils.getApplicationUrl(xmRequestContextHolder) + "/social-register/" + providerId)
                            .queryParam("success", "false").build().toString());
    }

}
