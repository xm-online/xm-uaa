package com.icthh.xm.uaa.security.oauth2.idp;

import com.icthh.xm.uaa.security.oauth2.idp.converter.CustomJwkVerifyingJwtAccessTokenConverter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Getter
@Setter
public class CustomJwkTokenStore extends JwtTokenStore {

    private CustomJwkVerifyingJwtAccessTokenConverter jwtTokenEnhancer;

    /**
     * Create a JwtTokenStore with this token enhancer (should be shared with the DefaultTokenServices if used).
     *
     * @param jwtTokenEnhancer
     */
    public CustomJwkTokenStore(CustomJwkVerifyingJwtAccessTokenConverter jwtTokenEnhancer) {
        super(jwtTokenEnhancer);
        this.jwtTokenEnhancer = jwtTokenEnhancer;
    }
}
