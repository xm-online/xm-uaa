package com.icthh.xm.uaa.security;

import com.icthh.xm.uaa.domain.Client;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class ClientDetailsImpl implements ClientDetails {

    private Client client;

    private Set<String> grantTypes;

    private Set<String> scope;

    private Set<String> registeredRedirectUris;

    private Set<String> autoApproveScopes;

    public ClientDetailsImpl(Client client, Set<String> grantTypes, Set<String> scope) {
        super();
        if (client == null) {
            throw new ClientRegistrationException("Client is null");
        }
        this.client = client;
        this.grantTypes = Collections.unmodifiableSet(grantTypes);

        Set<String> scopes = new HashSet<>(firstNonNull(client.getScopes(), emptySet()));
        scopes.addAll(scope);
        this.scope = Collections.unmodifiableSet(scopes);
    }

    public ClientDetailsImpl(Client client,
                             Set<String> grantTypes,
                             Set<String> scope,
                             Set<String> registeredRedirectUris,
                             Set<String> autoApproveScopes) {

        this(client, grantTypes, scope);
        this.registeredRedirectUris = Collections.unmodifiableSet(registeredRedirectUris);
        this.autoApproveScopes = Collections.unmodifiableSet(autoApproveScopes);
    }

    @Override
    public String getClientId() {
        return client.getClientId();
    }

    @Override
    public Set<String> getResourceIds() {
        return emptySet();
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
        return Objects.requireNonNullElse(registeredRedirectUris, emptySet());
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
        if (autoApproveScopes == null) {
            return false;
        }
        return autoApproveScopes.stream().anyMatch(auto -> auto.equals("true") || scope.matches(auto));
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return Collections.emptyMap();
    }
}
