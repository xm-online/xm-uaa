package com.icthh.xm.uaa.repository.util;

import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.uaa.domain.User;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_CREATED_DATE;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_ID;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_IMAGE_URL;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_NAME;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_USER_KEY;
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

        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertEquals(DEFAULT_LAST_MODIFIED_DATE, user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testMapWithoutDate() {
        User user = new User();
        SystemEventMapper.toUser(createEventWithoutDates(), user);

        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testOnlyFirstName() {
        User user = new User();

        SystemEvent event = createEvent();
        event.getDataMap().put(SYSTEM_EVENT_PROP_NAME, DEFAULT_FIRST_NAME);
        SystemEventMapper.toUser(event, user);

        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }

    @Test
    public void testEmptyName() {
        User user = new User();

        SystemEvent event = createEvent();
        event.getDataMap().put(SYSTEM_EVENT_PROP_NAME, "");
        SystemEventMapper.toUser(event, user);

        assertEquals(DEFAULT_IMAGE_URL, user.getImageUrl());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(DEFAULT_USER_LOGIN, user.getLastModifiedBy());
    }


    private SystemEvent createEvent() {
        SystemEvent event = createEventWithoutDates();
        event.getDataMap().put(SYSTEM_EVENT_PROP_CREATED_DATE, DEFAULT_CREATED_DATE.toString());
        event.getDataMap().put(SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE, DEFAULT_LAST_MODIFIED_DATE.toString());

        return event;
    }

    private SystemEvent createEventWithoutDates() {
        SystemEvent event = new SystemEvent();
        event.setUserLogin(DEFAULT_USER_LOGIN);
        event.setTenantKey(null);

        Map<String, Object> data = new HashMap<>();
        data.put(SYSTEM_EVENT_PROP_ID, DEFAULT_ID);
        data.put(SYSTEM_EVENT_PROP_NAME, DEFAULT_FIRST_NAME + " " + DEFAULT_LAST_NAME);
        data.put(SYSTEM_EVENT_PROP_IMAGE_URL, DEFAULT_IMAGE_URL);
        data.put(SYSTEM_EVENT_PROP_CREATED_DATE, "");
        data.put(SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE, "");
        data.put(SYSTEM_EVENT_PROP_USER_KEY, DEFAULT_USER_KEY);

        event.setData(data);
        return event;

    }
}
