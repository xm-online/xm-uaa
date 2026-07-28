package com.icthh.xm.uaa.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenValidity {

    private final int accessTokenValiditySeconds;

    private final int refreshTokenValiditySeconds;
}
