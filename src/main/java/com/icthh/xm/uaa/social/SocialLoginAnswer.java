package com.icthh.xm.uaa.social;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@Builder
@Getter
public class SocialLoginAnswer {

    private AnswerType answerType;
    private OAuth2AccessToken oAuth2AccessToken;
    private String activationCode;

    public enum AnswerType {
        SING_IN, REGISTERED, NEED_ACCEPT_CONNECTION, NEED_ACCEPT_ACCOUNT_CREATION;
    }

}
