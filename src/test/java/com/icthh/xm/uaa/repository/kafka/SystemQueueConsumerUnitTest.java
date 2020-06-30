package com.icthh.xm.uaa.repository.kafka;

import com.google.common.collect.ImmutableSet;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.UserService;
import com.icthh.xm.uaa.service.permission.PermissionUpdateService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class SystemQueueConsumerUnitTest {

    private static final String USER_KEY = "f81d3142-a259-4ff8-99e4-be533d68ca99";

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

    @Mock
    private PermissionUpdateService permissionUpdateService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(authContext);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);

        consumer = new SystemQueueConsumer(tenantContextHolder, authContextHolder, lepManager, userService, permissionUpdateService);
    }

    @Test
    public void updateProfile() {
        when(authContext.getLogin()).thenReturn(Optional.empty());
        when(userService.getUser(USER_KEY)).thenReturn(new User());
        doNothing().when(userService).saveUser(any());

        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "",
            readFromFile("/SystemQueueConsumerUnitTest/update_account_event.json")));

        verify(userService).getUser(USER_KEY);
        verify(userService).saveUser(any());
    }

    @Test
    public void updateNotExistsProfile() {
        when(authContext.getLogin()).thenReturn(Optional.empty());
        when(userService.getUser(USER_KEY)).thenReturn(null);
        doNothing().when(userService).saveUser(any());
        consumer.consumeEvent(new ConsumerRecord<>("test", 0, 0, "",
            readFromFile("/SystemQueueConsumerUnitTest/update_account_event.json")));

        verify(userService).getUser(USER_KEY);
        verify(userService, times(0)).saveUser(any());
    }

    @Test
    public void deletePrivileges() {
        //given
        String event = readFromFile("/SystemQueueConsumerUnitTest/ms_privileges.json");
        String msName = "test-ms";

        //when
        consumer.consumeEvent(new ConsumerRecord<>(msName, 0, 0, "", event));

        //then
        verify(permissionUpdateService).deleteRemovedPrivileges(msName, ImmutableSet.of(
            newPrivilege("ACCOUNT.ACTIVATE", "Privilege to activate the registered user",
                msName, null),
            newPrivilege("ACCOUNT.CHECK_AUTH", "Privilege to check if the user is authenticated",
                msName, Collections.singleton("request"))
        ));
    }

    private Privilege newPrivilege(String key, String description, String msName, Set<String> resources) {
        Privilege privilege = new Privilege();
        privilege.setKey(key);
        privilege.setCustomDescription(description);
        privilege.setMsName(msName);
        privilege.setResources(resources);
        return privilege;
    }

    @SneakyThrows
    private String readFromFile(String fileName) {
        return IOUtils.resourceToString(fileName, UTF_8);
    }

}
