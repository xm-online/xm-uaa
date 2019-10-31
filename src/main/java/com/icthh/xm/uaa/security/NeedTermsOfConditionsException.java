package com.icthh.xm.uaa.security;

import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;

public class NeedTermsOfConditionsException extends ClientAuthenticationException {

    public static final String ONE_TIME_TOKEN = "oneTimeToken";

    public NeedTermsOfConditionsException(String token) {
        super("Need accept terms of conditions");
        addAdditionalInformation(ONE_TIME_TOKEN, token);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "needAcceptTermsOfConditions";
    }

    public String getOneTimeToken() {
        return getAdditionalInformation().get(ONE_TIME_TOKEN);
    }
}
