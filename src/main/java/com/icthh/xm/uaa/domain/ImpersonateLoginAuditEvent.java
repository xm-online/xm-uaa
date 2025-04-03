package com.icthh.xm.uaa.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "impersonate_login_audit_event")
public class ImpersonateLoginAuditEvent implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "inbound_login", nullable = false)
    private String inboundLogin;

    @NotNull
    @Column(name = "inbound_tenant", nullable = false)
    private String inboundTenant;

    @NotNull
    @Column(name = "inbound_role", nullable = false)
    private String inboundRole;

    @NotNull
    @Column(name = "inbound_user_key", nullable = false)
    private String inboundUserKey;

    @NotNull
    @Column(name = "user_login", nullable = false)
    private String userLogin;

    @NotNull
    @Column(name = "user_key", nullable = false)
    private String userKey;

    @NotNull
    @Column(name = "user_role", nullable = false)
    private String userRole;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;

}
