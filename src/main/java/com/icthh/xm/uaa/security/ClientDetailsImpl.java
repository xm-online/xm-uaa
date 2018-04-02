package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.domain.Client;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

public class ClientDetailsImpl implements ClientDetails {

    private Client client;

    private Set<String> grantTypes;

    private Set<String> scope;

    public ClientDetailsImpl(Client client, Set<String> grantTypes, Set<String> scope) {
        super();
        if (client == null) {
            throw new ClientRegistrationException("Client is null");
        }
        this.client = client;
        this.grantTypes = Collections.unmodifiableSet(grantTypes);
        this.scope = Collections.unmodifiableSet(scope);
    }

    @Override
    public String getClientId() {
        return client.getClientId();
    }

    @Override
    public Set<String> getResourceIds() {
        return Collections.emptySet();
    }

    @Override
    public boolean isSecretRequired() {
        return false;
    }

    @Override
    public String getClientSecret() {
        return client.getClientSecret();
    }

    @Override
    public boolean isScoped() {
        return false;
    }

    @Override
    public Set<String> getScope() {
        return scope;
    }

    @Override
    public Set<String> getAuthorizedGrantTypes() {
        return grantTypes;
    }

    @Override
    public Set<String> getRegisteredRedirectUri() {
        return Collections.emptySet();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(client.getRoleKey()));
    }

    @Override
    public Integer getAccessTokenValiditySeconds() {
        return client.getAccessTokenValiditySeconds();
    }

    @Override
    public Integer getRefreshTokenValiditySeconds() {
        return null;
    }

    @Override
    public boolean isAutoApprove(String scope) {
        return false;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return Collections.emptyMap();
    }
}
