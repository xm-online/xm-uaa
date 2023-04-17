package com.icthh.xm.uaa.security.oauth2.tfa;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;

/**
 * The {@link TfaOtpMsAuthenticationToken} class.
 */
public class TfaOtpMsAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private OtpMsCredentials credentials;

    public TfaOtpMsAuthenticationToken(Object principal, OtpMsCredentials otpCredentials) {
        super(null);
        this.principal = principal;
        this.credentials = otpCredentials;
        setAuthenticated(false);
    }

    public TfaOtpMsAuthenticationToken(Object principal,
                                       OtpMsCredentials otpCredentials,
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
    public OtpMsCredentials getCredentials() {
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

    @Getter
    public static class OtpMsCredentials implements CredentialsContainer, Serializable {

        private String otp;
        private Long otpId;

        public OtpMsCredentials(String otp, Long otpId) {
            this.otp = otp;
            this.otpId = otpId;
        }

        @Override
        public void eraseCredentials() {
            this.otp = null;
            this.otpId = null;
        }

    }

}
