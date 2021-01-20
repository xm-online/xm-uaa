package com.icthh.xm.uaa.security.oauth2.idp.config;

import lombok.Data;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.uaa.security.oauth2.idp.config.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;

@Data
public class IdpConfigContainer {

    private IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateClientConfig idpPrivateClientConfig;

    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }

}
