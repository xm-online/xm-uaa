package com.icthh.xm.uaa.service.dto;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
public class RoleMatrixDTO {

    private Collection<String> roles;
    private Set<PermissionMatrixDTO> permissions = new TreeSet<>();

    @Override
    public String toString() {
        return "RoleMatrixDTO{" +
               "roles=" + roles +
               ", permissions.size=" + permissions.size() +
               '}';
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    @EqualsAndHashCode(exclude = "roles")
    @ToString(exclude = "roles")
    public static class PermissionMatrixDTO implements Comparable<PermissionMatrixDTO> {

        private String msName;
        private String privilegeKey;
        private PermissionType permissionType;
        private Set<String> roles = new TreeSet<>();

        @Override
        public int compareTo(PermissionMatrixDTO o) {
            return Comparator.comparing(PermissionMatrixDTO::getMsName)
                .thenComparing(PermissionMatrixDTO::getPrivilegeKey)
                .compare(this, o);
        }
    }

}
