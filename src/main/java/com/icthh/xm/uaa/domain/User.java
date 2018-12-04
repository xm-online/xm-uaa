package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.uaa.domain.converter.MapToStringConverter;
import com.icthh.xm.uaa.repository.converter.OtpChannelTypeAttributeConverter;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jboss.aerogear.security.otp.api.Base32;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A user.
 */
@Entity
@Table(name = "jhi_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
public class User extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "user_key", unique = true, nullable = false)
    private String userKey;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", length = 60)
    private String password;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull
    @Column(nullable = false)
    private boolean activated = false;

    @NotNull
    @Column(name = "tfa_enabled")
    private boolean tfaEnabled = false;

    @Convert(converter = OtpChannelTypeAttributeConverter.class)
    @Column(name = "tfa_otp_channel_type", length = 64)
    private OtpChannelType tfaOtpChannelType;

    @Transient
    private transient String tfaOtpSecret = Base32.random();

    @Size(min = 2, max = 5)
    @Column(name = "lang_key", length = 5)
    private String langKey = "en";

    @Size(max = 256)
    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    @JsonIgnore
    private String activationKey;

    @Column(name = "create_activation_key_date")
    @JsonIgnore
    private Instant createActivationKeyDate;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    @JsonIgnore
    private String resetKey;

    @Column(name = "reset_date")
    private Instant resetDate = null;

    @Column(name = "access_token_validity_seconds")
    private Integer accessTokenValiditySeconds;

    @Column(name = "refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds;

    @Column(name = "tfa_access_token_validity")
    private Integer tfaAccessTokenValiditySeconds;

    @Column(name = "role_key")
    private String roleKey;

    @Convert(converter = MapToStringConverter.class)
    @Column(name = "data")
    private Map<String, Object> data = new HashMap<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLogin> logins = new ArrayList<>();

    @NotNull
    @Column(name = "auto_logout_enabled")
    private boolean autoLogoutEnabled = false;

    @Column(name = "auto_logout_timeout_seconds")
    private Integer autoLogoutTimeoutSeconds;

    // TODO refactor, put EMAIL type to configuration
    public String getEmail() {
        return getLogins().stream().filter(userLogin -> UserLoginType.EMAIL.getValue().equals(userLogin.getTypeKey()))
            .findFirst().map(UserLogin::getLogin).orElse(null);
    }

    public void setActivationKey(String key) {
        this.activationKey = key;
        this.createActivationKeyDate = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;
        return !(user.getId() == null || getId() == null) && Objects.equals(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "User{"
            + "userKey='" + userKey + '\''
            + ", firstName='" + firstName + '\''
            + ", lastName='" + lastName + '\''
            + ", imageUrl='" + imageUrl + '\''
            + ", activated='" + activated + '\''
            + ", tfaEnabled='" + tfaEnabled + '\''
            + ", tfaOtpChannelType='" + tfaOtpChannelType + '\''
            + ", autoLogoutEnabled='" + autoLogoutEnabled + '\''
            + ", autoLogoutTimeoutSeconds='" + autoLogoutTimeoutSeconds + '\''
            + ", langKey='" + langKey + '\''
            + ", activationKey='" + activationKey + '\''
            + ", roleKey='" + roleKey + '\''
            + "}";
    }
}
