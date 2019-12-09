package com.icthh.xm.uaa.domain;

import com.icthh.xm.uaa.domain.converter.ListToStringConverter;
import lombok.Getter;
import lombok.Setter;
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

    @Column(name = "access_token_validity")
    private Integer accessTokenValiditySeconds;

    @Convert(converter = ListToStringConverter.class)
    @Getter
    @Setter
    private List<String> scopes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public Client clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Client clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public Client roleKey(String roleKey) {
        this.roleKey = roleKey;
        return this;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getDescription() {
        return description;
    }

    public Client description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(Integer accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public void scopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> scopes() {
        return scopes;
    }

    @Override
    public String toString() {
        return "Client{"
            + "id=" + getId()
            + ", clientId='" + getClientId() + "'"
            + ", clientSecret='" + getClientSecret() + "'"
            + ", roleKey='" + getRoleKey() + "'"
            + ", description='" + getDescription() + "'"
            + ", accessTokenValiditySeconds='" + getAccessTokenValiditySeconds() + "'"
            + "}";
    }
}
