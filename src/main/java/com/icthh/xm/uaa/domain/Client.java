package com.icthh.xm.uaa.domain;

import com.icthh.xm.uaa.domain.converter.ListToStringConverter;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.List;

/**
 * A Client.
 */
@Entity
@Table(name = "client")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@ToString(exclude = {"scopes"})
public class Client extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "client_id", unique = true)
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "role_key")
    private String roleKey;

    @Column(name = "description")
    private String description;

    @Column(name="state")
    @Enumerated(EnumType.STRING)
    private ClientState state;

    @Column(name = "access_token_validity")
    private Integer accessTokenValiditySeconds;

    @Convert(converter = ListToStringConverter.class)
    @Getter
    @Setter
    private List<String> scopes;

    public Client clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public Client clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public Client roleKey(String roleKey) {
        this.roleKey = roleKey;
        return this;
    }

    public Client description(String description) {
        this.description = description;
        return this;
    }

    public void scopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> scopes() {
        return scopes;
    }


}
