package com.icthh.xm.uaa.security;

import static java.util.UUID.randomUUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class ImpersonateAuthenticationToken extends UsernamePasswordAuthenticationToken {
    public ImpersonateAuthenticationToken(Object principal) {
        super(principal, randomUUID().toString());
    }
}
