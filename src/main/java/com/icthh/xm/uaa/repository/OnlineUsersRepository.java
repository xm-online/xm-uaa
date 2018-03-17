package com.icthh.xm.uaa.repository;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Repository which stores online users. Uses hazelcast as storage. Entries stores in 'users_online'
 * map.
 */
@Slf4j
@Component
public class OnlineUsersRepository {

    private static final String USERS_ONLINE_MAP = "users_online";

    private final HazelcastInstance hazelcast;

    public OnlineUsersRepository(
                    @Qualifier("hazelcastInstance") HazelcastInstance hazelcast) {
        this.hazelcast = hazelcast;
    }

    /**
     * This map stores online users by key with format: {tenant}:{userKey} and value: {JWT
     * identifier}.
     * @return distributed map instance
     */
    private IMap<String, String> getMap() {
        return hazelcast.getMap(USERS_ONLINE_MAP);
    }

    /**
     * Save new online user in repository.
     * @param key key format: {tenant}:{userKey}
     * @param jti JWT identifier
     * @param timeToLive the amount of time during which the record will stores in repository
     */
    public void save(String key, String jti, long timeToLive) {
        log.debug("Save authenticated user to hazelcast with key:{}, value:{}, timeToLive:{}", key,
                        jti, timeToLive);
        getMap().put(key, jti, timeToLive, TimeUnit.SECONDS);
    }

    public Collection<String> find(String key) {
        log.debug("Get authenticated user from hazelcast by key expression {}", key);
        SqlPredicate predicate = new SqlPredicate("__key like " + key);
        return getMap().values(predicate);
    }

    public Collection<String> findAll() {
        log.debug("Get all authenticated users from hazelcast");
        Set<Map.Entry<String, String>> entries = getMap().entrySet();
        if (CollectionUtils.isEmpty(entries)) {
            return Collections.emptyList();
        }
        List<String> onlineUsers = new LinkedList<>();
        for (Map.Entry<String, String> entry : entries) {
            onlineUsers.add(entry.getValue());
        }
        return onlineUsers;
    }

    public void delete(String key) {
        log.debug("Delete authenticated user from hazelcast by key {}", key);
        getMap().delete(key);
    }

    public void deleteAll() {
        getMap().clear();
    }
}
