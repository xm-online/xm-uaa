package com.icthh.xm.uaa.security.oauth2.tfa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * The {@link TfaOtpAuthenticationToken} class.
 */
public class TfaOtpAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private OtpCredentials credentials;

    public TfaOtpAuthenticationToken(Object principal, OtpCredentials otpCredentials) {
        super(null);
        this.principal = principal;
        this.credentials = otpCredentials;
        setAuthenticated(false);
    }

    public TfaOtpAuthenticationToken(Object principal,
                                     OtpCredentials otpCredentials,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = otpCredentials;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }


    @Override
    public OtpCredentials getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }

    @AllArgsConstructor
    @Getter
    public static class OtpCredentials implements CredentialsContainer {

        private String otp;
        private String encodedOtp;

        @Override
        public void eraseCredentials() {
            this.otp = null;
            this.encodedOtp = null;
        }

    }

}
