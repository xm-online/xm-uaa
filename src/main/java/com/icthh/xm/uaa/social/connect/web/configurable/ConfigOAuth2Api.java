package com.icthh.xm.uaa.social.connect.web.configurable;

import lombok.RequiredArgsConstructor;
import org.springframework.social.ApiBinding;

@RequiredArgsConstructor
public class ConfigOAuth2Api implements ApiBinding {
    private final String accessToken;
    @Override
    public boolean isAuthorized() {
        return accessToken != null;
    }

    public boolean test() {
        return true;
    }
}
