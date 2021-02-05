package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.ClientState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@ToString
@Getter
@Setter
@NoArgsConstructor
public class ClientDTO {

    private Long id;
    private String clientId;
    private String clientSecret;
    private String roleKey;
    private String description;
    private ClientState clientState;
    private Integer accessTokenValiditySeconds;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
    private List<String> scopes;

    public ClientDTO(Client client) {
        this(client.getId(), client.getClientId(), client.getClientSecret(), client.getRoleKey(),
        client.getDescription(), client.getState(), client.getAccessTokenValiditySeconds(), client.getCreatedBy(),
        client.getCreatedDate(), client.getLastModifiedBy(), client.getLastModifiedDate(), client.getScopes());
    }
}
