package com.icthh.xm.uaa.repository;

import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.uaa.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

@Repository
public class UserPermittedRepository extends PermittedRepository {

    public UserPermittedRepository(PermissionCheckService permissionCheckService) {
        super(permissionCheckService);
    }

    /**
     * Find all permitted users by role key.
     * @param pageable the page info
     * @param roleKey the role key
     * @param privilegeKey the privilege key
     * @return permitted users
     */
    public Page<User> findAllByRoleKey(Pageable pageable, String roleKey, String privilegeKey) {
        String whereCondition = "roleKey = :roleKey";

        Map<String, Object> conditionParams = Collections.singletonMap("roleKey", roleKey);

        return findByCondition(whereCondition, conditionParams, pageable, getType(), privilegeKey);
    }

    private Class<User> getType() {
        return User.class;
    }

    public Page<User> findAllByUserKeys(Pageable pageable, List<String> userKeys, String privilegeKey) {
        String whereCondition = "userKey in :userKeys";
        Map<String, Object> conditionParams = Collections.singletonMap("userKeys", userKeys);
        return findByCondition(whereCondition, conditionParams, pageable, getType(), privilegeKey);
    }
}
