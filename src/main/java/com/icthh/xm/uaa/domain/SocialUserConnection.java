package com.icthh.xm.uaa.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * A Social user.
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jhi_social_user_connection")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class SocialUserConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "user_key", nullable = false)
    private String userKey;

    @NotNull
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @NotNull
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "profile_url")
    private String profileURL;

}
