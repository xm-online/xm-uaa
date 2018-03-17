package com.icthh.xm.uaa.repository.kafka;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

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


    private SystemQueueConsumer consumer;

    @Mock
    private UserService userService;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext authContext;

    @Mock
    private PrivilegedTenantContext privilegedTenantContext;

    @Mock
    private LepManager lepManager;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(authContext);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);

        consumer = new SystemQueueConsumer(tenantContextHolder, authContextHolder, lepManager, userService);
    }

    @After
    public void destroy() {
    }

    @Test
    public void updateProfile() {
        when(authContext.getLogin()).thenReturn(Optional.empty());
        when(userService.getUser(USER_KEY)).thenReturn(new User());
        doNothing().when(userService).saveUser(anyObject());

        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_ACCOUNT_EVENT));

        verify(userService).getUser(USER_KEY);
        verify(userService).saveUser(anyObject());
    }

    @Test
    public void updateNotExistsProfile() {
        when(authContext.getLogin()).thenReturn(Optional.empty());
        when(userService.getUser(USER_KEY)).thenReturn(null);
        doNothing().when(userService).saveUser(anyObject());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "", UPDATE_ACCOUNT_EVENT));

        verify(userService).getUser(USER_KEY);
        verify(userService, times(0)).saveUser(anyObject());
    }

}
