package com.icthh.xm.uaa.repository.kafka;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.config.tenant.TenantContext;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class SystemQueueConsumerUnitTest {

    private static final String USER_KEY = "f81d3142-a259-4ff8-99e4-be533d68ca99";

    private static final String UPDATE_ACCOUNT_EVENT = "{  \n" +
        "   \"eventId\":\"f81d3142-a259-4ff8-99e4-be533d68ca88\",\n" +
        "   \"messageSource\":\"ms-uaa\",\n" +
        "   \"tenantInfo\":{  \n" +
        "      \"tenant\":\"XM\",\n" +
        "      \"xmToken\":\"\",\n" +
        "      \"xmCookie\":\"\",\n" +
        "      \"xmUserId\":\"\",\n" +
        "      \"xmLocale\":\"en\",\n" +
        "      \"userLogin\":\"\",\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   },\n" +
        "   \"eventType\":\"UPDATE_ACCOUNT\",\n" +
        "   \"startDate\":\"2017-11-20T13:15:30Z\",\n" +
        "   \"data\":{  \n" +
        "      \"id\":1234,\n" +
        "      \"firstName\":\"Olena\",\n" +
        "      \"lastName\":\"Kashyna\",\n" +
        "      \"imageUrl\":\"\",\n" +
        "      \"activated\":true,\n" +
        "      \"langKey\":\"en\",\n" +
        "      \"createdBy\":\"system\",\n" +
        "      \"createdDate\":\"2017-11-20T13:15:30Z\",\n" +
        "      \"lastModifiedBy\":\"\",\n" +
        "      \"lastModifiedDate\":\"\",\n" +
        "\n" +
        "      \"userKey\":\"" + USER_KEY + "\"\n" +
        "   }\n" +
        "}";

    private UserService userService;

    private SystemQueueConsumer consumer;

    @Before
    public void init() {
        userService = mock(UserService.class);
        consumer = new SystemQueueConsumer(userService);
        TenantContext.setCurrent("TEST");
    }

    @Test
    public void updateProfile() {
        when(userService.getUser(USER_KEY)).thenReturn(new User());
        doNothing().when(userService).saveUser(anyObject());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_ACCOUNT_EVENT));

        verify(userService).getUser(USER_KEY);
        verify(userService).saveUser(anyObject());
    }

    @Test
    public void updateNotExistsProfile() {
        when(userService.getUser(USER_KEY)).thenReturn(null);
        doNothing().when(userService).saveUser(anyObject());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_ACCOUNT_EVENT));

        verify(userService).getUser(USER_KEY);
        verify(userService, times(0)).saveUser(anyObject());
    }

}
