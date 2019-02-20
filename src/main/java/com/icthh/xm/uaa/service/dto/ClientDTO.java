package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.Client;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

/**
 * Client DTO representing.
 */
@AllArgsConstructor
@ToString
@Getter
@Setter
public class ClientDTO {

    private Long id;

    private String clientId;

    private String clientSecret;

    private String roleKey;

    private String description;

    private Integer accessTokenValiditySeconds;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    private List<String> scopes;

    @SuppressWarnings("unused")
    public ClientDTO() {
        // Empty constructor needed for Jackson.
    }

    /**
     * ClientDTO constructor.
     *
     * @param client client
     */
    public ClientDTO(Client client) {
        this(client.getId(), client.getClientId(), client.getClientSecret(), client.getRoleKey(),
        client.getDescription(), client.getAccessTokenValiditySeconds(), client.getCreatedBy(),
        client.getCreatedDate(), client.getLastModifiedBy(), client.getLastModifiedDate(), client.getScopes());
    }
}
