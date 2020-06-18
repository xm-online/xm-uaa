package com.icthh.xm.uaa.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * //todo V: add doc
 */
@Entity
@Table(name = "role")
//@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE) // //todo V!: needed?
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = true) //todo V!: needed? use just id?
@ToString(exclude = "permissions")
public class Role extends AbstractAuditingEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "role_key")
    private String roleKey;

    @OneToOne
    @JoinColumn(name = "based_on")
    private Role basedOn;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "role")
    private List<Permission> permissions; //todo V: use set?

}
