package com.icthh.xm.uaa.repository.util;

import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.config.tenant.TenantInfo;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.kafka.SystemEvent;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SystemEventMapperUnitTest {

    private static final String DEFAULT_ID = "id";
    private static final String DEFAULT_FIRST_NAME = "firstName";
    private static final String DEFAULT_LAST_NAME = "lastName";
    private static final String DEFAULT_IMAGE_URL = "imageUrl";
    private static final Instant DEFAULT_CREATED_DATE = Instant.now();
    private static final Instant DEFAULT_LAST_MODIFIED_DATE = Instant.now();
    private static final String DEFAULT_USER_KEY = "userKey";

    private static final String DEFAULT_USER_LOGIN = "login";

    @Test
    public void testMap() {
        User user = new User();
        SystemEventMapper.toUser(createEvent(), user);

        assertEquals(DEFAULT_FIRST_NAME, user.getFirstName());
        assertEquals(DEFAULT_LAST_NAME, user.getLastName());
        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertEquals(DEFAULT_LAST_MODIFIED_DATE, user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testMapWithoutDate() {
        User user = new User();
        SystemEventMapper.toUser(createEventWithoutDates(), user);

        assertEquals(DEFAULT_FIRST_NAME, user.getFirstName());
        assertEquals(DEFAULT_LAST_NAME, user.getLastName());
        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testOnlyFirstName() {
        User user = new User();

        SystemEvent event = createEvent();
        event.getData().put(Constants.NAME, DEFAULT_FIRST_NAME);
        SystemEventMapper.toUser(event, user);

        assertEquals(DEFAULT_FIRST_NAME, user.getFirstName());
        assertEquals("", user.getLastName());
        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testEmptyName() {
        User user = new User();

        SystemEvent event = createEvent();
        event.getData().put(Constants.NAME, "");
        SystemEventMapper.toUser(event, user);

        assertEquals("", user.getFirstName());
        assertEquals("", user.getLastName());
        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }


    private SystemEvent createEvent() {
        SystemEvent event = createEventWithoutDates();
        event.getData().put(Constants.CREATED_DATE, DEFAULT_CREATED_DATE.toString());
        event.getData().put(Constants.LAST_MODIFIED_DATE, DEFAULT_LAST_MODIFIED_DATE.toString());

        return event;
    }

    private SystemEvent createEventWithoutDates() {
        SystemEvent event = new SystemEvent();
        Map<String, Object> data = new HashMap<>();
        event.setData(data);
        data.put(Constants.ID, DEFAULT_ID);
        data.put(Constants.NAME, DEFAULT_FIRST_NAME + " " + DEFAULT_LAST_NAME);
        data.put(Constants.IMAGE_URL, DEFAULT_IMAGE_URL);
        data.put(Constants.CREATED_DATE, "");
        data.put(Constants.LAST_MODIFIED_DATE, "");
        data.put(Constants.USER_KEY, DEFAULT_USER_KEY);

        TenantInfo info  = new TenantInfo(null, DEFAULT_USER_LOGIN, null, null, null, null, null);
        event.setTenantInfo(info);

        return event;

    }
}
