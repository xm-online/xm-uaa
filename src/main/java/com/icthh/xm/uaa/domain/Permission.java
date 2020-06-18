package com.icthh.xm.uaa.domain;

import com.icthh.xm.uaa.service.dto.PermissionType;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CollectionType;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * //todo V:
 */
@Entity
@Table(name = "permission")
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE) // //todo V!: needed?
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class Permission extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne
    private Role role;

    @Column(name = "ms_name")
    private String msName;

    @Column(name = "privilege_key")
    private String privilegeKey;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "reaction_strategy")
    private String reactionStrategy;

    @Column(name = "env_condition")
    private String envCondition;

    @Column(name = "resource_condition")
    private String resourceCondition;

//    //todo V!: needed?
//    @ElementCollection
//    private Set<String> resources; //todo V!: relation?

    @Column(name = "permission_type")
    @Enumerated(EnumType.STRING)
    private PermissionType permissionType;

    @Column(name = "description")
    private String description;

}
