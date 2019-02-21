package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "user_login")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@ToString(exclude = {"user"})
@Getter
@Setter
@EqualsAndHashCode(exclude = {"user"})
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
