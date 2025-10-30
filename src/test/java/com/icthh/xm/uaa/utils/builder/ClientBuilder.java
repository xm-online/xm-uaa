package com.icthh.xm.uaa.utils.builder;

import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.ClientState;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class ClientBuilder {

    private static final String ROLE_USER = "ROLE_USER";

    private Client client = new Client();

    public ClientBuilder builder() {
        String clientId = "test-client-" + RandomStringUtils.randomAlphanumeric(10);

        client.clientId(clientId);
        client.clientSecret(clientId);
        client.roleKey(ROLE_USER);
        client.description(RandomStringUtils.randomAlphanumeric(20));
        client.setState(ClientState.ACTIVE);

        return this;
    }

    public Client build() {
        return client;
    }

    public ClientBuilder withState(ClientState state) {
        client.setState(state);
        return this;
    }

    public ClientBuilder withScope(List<String> scopes) {
        client.setScopes(scopes);
        return this;
    }

    public ClientBuilder withRole(String role) {
        client.setRoleKey(role);
        return this;
    }
}
