package com.icthh.xm.uaa.repository;

import com.icthh.xm.uaa.domain.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    @Query("DELETE FROM PermissionEntity p " +
        "WHERE p.msName = :msName " +
        "  AND p.privilegeKey NOT IN (:privilegeKeys)")
    @Modifying
    void deletePermissionsNotIn(@Param("msName") String msName,
                                @Param("privilegeKeys") Collection<String> privilegeKeys);

    @Query("DELETE FROM PermissionEntity p " +
        "WHERE p.msName = :msName")
    @Modifying
    void deletePermissionsByMsName(@Param("msName") String msName);
}
