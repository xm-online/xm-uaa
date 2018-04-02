package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "user_login")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@ToString(exclude = {"user"})
@Getter
@Setter
@EqualsAndHashCode
public class UserLogin {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "type_key", length = 50, nullable = false)
    @NotNull
    private String typeKey;

    @Column(name = "state_key", length = 50)
    private String stateKey;

    @Column(nullable = false, unique = true)
    @NotBlank
    private String login;

    @Column(nullable = false)
    private boolean removed = false;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_key", referencedColumnName = "user_key")
    private User user;

}
