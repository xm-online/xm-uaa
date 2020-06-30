package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.uaa.service.permission.TenantRoleService;
import com.icthh.xm.uaa.service.dto.RoleDTO;
import com.icthh.xm.uaa.service.dto.RoleMatrixDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * REST controller for managing Role.
 */
@RestController
@RequestMapping("/api")
public class RoleResource {

    private final TenantRoleService tenantRoleService;

    public RoleResource(TenantRoleService tenantRoleService) {
        this.tenantRoleService = tenantRoleService;
    }

    /**
     * POST /clients : Create a new role.
     * @param role the role to create
     * @return the ResponseEntity with status 201 (Created) and with body the new client, or with
     *         status 400 (Bad Request) if the client has already an ID
     */
    @PostMapping("/roles")
    @Timed
    @PreAuthorize("hasPermission({'role': #role}, 'ROLE.CREATE')")
    @PrivilegeDescription("Privilege to create a new role")
    public ResponseEntity<Void> createRole(@RequestBody RoleDTO role) {
        tenantRoleService.addRole(role);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /roles : Get roles.
     * @return the ResponseEntity with status 200 (Ok) and with body the roles, or with
     *         status 400 (Bad Request)
     */
    @GetMapping("/roles")
    @Timed
    @PostFilter("hasPermission({'returnObject': filterObject, 'log': false}, 'ROLE.GET_LIST')")
    @PrivilegeDescription("Privilege to get all roles")
    public Collection<RoleDTO> getRoles() {
        return tenantRoleService.getAllRoles();
    }

    /**
     * GET /role : Get role by role key.
     * @param roleKey the role key
     * @return the ResponseEntity with status 200 (OK) and with body the "roleKey" role, or with status 404 (Not Found)
     */
    @GetMapping("/roles/{roleKey}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ROLE.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get the role by roleKey")
    public ResponseEntity<RoleDTO> getRole(@PathVariable String roleKey) {
        return ResponseUtil.wrapOrNotFound(tenantRoleService.getRole(roleKey));
    }


    /**
     * DELETE /roles/:roleKey : delete the "roleKey" Role.
     *
     * @param roleKey the role key of the role to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/roles/{roleKey}")
    @Timed
    @PreAuthorize("hasPermission({'roleKey': #roleKey}, 'ROLE.DELETE')")
    @PrivilegeDescription("Privilege to delete the role by roleKey")
    public ResponseEntity<Void> deleteUser(@PathVariable String roleKey) {
        tenantRoleService.deleteRole(roleKey);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /roles : Update role.
     * @param roleDTO the role to update
     * @return the ResponseEntity with status 200 (OK) or with status 404 (Not Found)
     */
    @PutMapping("/roles")
    @Timed
    @PreAuthorize("hasPermission({'role': #roleDTO}, 'ROLE.UPDATE')")
    @PrivilegeDescription("Privilege to update an existing role")
    public ResponseEntity<Void> updateRole(@RequestBody RoleDTO roleDTO) {
        tenantRoleService.updateRole(roleDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /role/matrix : Get role matrix.
     * @return the ResponseEntity with status 200 (OK) and with body the role matrix, or with status 400 (Bad Request)
     */
    @GetMapping("/roles/matrix")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ROLE.MATRIX.GET')")
    @PrivilegeDescription("Privilege to get role matrix")
    public ResponseEntity<RoleMatrixDTO> getRoleMatrix() {
        return ResponseEntity.ok(tenantRoleService.getRoleMatrix());
    }

    /**
     * PUT /roles/matrix : Update permissions by role matrix.
     * @param roleMatrix the role matrix to update
     * @return the ResponseEntity with status 200 (OK) or with status 400 (Bad Request)
     */
    @PutMapping("/roles/matrix")
    @Timed
    @PreAuthorize("hasPermission({'roleMatrix': #roleMatrix}, 'ROLE.MATRIX.UPDATE')")
    @PrivilegeDescription("Privilege to update permissions by role matrix")
    public ResponseEntity<Void> updateRoleMatrix(@RequestBody RoleMatrixDTO roleMatrix) {
        tenantRoleService.updateRoleMatrix(roleMatrix);
        return ResponseEntity.ok().build();
    }

}
