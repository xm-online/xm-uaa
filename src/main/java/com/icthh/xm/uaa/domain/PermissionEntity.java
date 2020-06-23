package com.icthh.xm.uaa.domain;

import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Persistence entity for Permissions
 */
@Entity
@Table(name = "permission")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true, exclude = "role")
@ToString(exclude = "role")
@Builder
public class PermissionEntity extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne
    private RoleEntity role;

    @Column(name = "ms_name")
    private String msName;

    @Column(name = "privilege_key")
    private String privilegeKey;

    @Column(name = "disabled")
    private boolean disabled;

    @Column(name = "reaction_strategy")
    @Enumerated(value = EnumType.STRING)
    private ReactionStrategy reactionStrategy;

    @Column(name = "env_condition")
    private String envCondition;

    @Column(name = "resource_condition")
    private String resourceCondition;

}
