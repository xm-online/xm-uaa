package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.ClientState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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
    @Pattern(regexp = "^(?!SUPER-ADMIN$).*$", message = "Role not allowed")
    private String roleKey;
    @Size(max = 500)
    private String description;
    private ClientState clientState;
    private Integer accessTokenValiditySeconds;
    private Integer refreshTokenValiditySeconds;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
    private List<String> scopes;

    public ClientDTO(Client client) {
        this(client.getId(), client.getClientId(), client.getClientSecret(), client.getRoleKey(),
        client.getDescription(), client.getState(), client.getAccessTokenValiditySeconds(),
        client.getRefreshTokenValiditySeconds(), client.getCreatedBy(),
        client.getCreatedDate(), client.getLastModifiedBy(), client.getLastModifiedDate(), client.getScopes());
    }
}
