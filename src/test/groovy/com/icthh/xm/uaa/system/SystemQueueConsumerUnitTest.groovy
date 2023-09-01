package com.icthh.xm.uaa.system

import com.icthh.xm.commons.messaging.event.system.SystemEvent
import com.icthh.xm.uaa.AbstractGroovyUnitTest
import com.icthh.xm.uaa.LepTestConstants
import com.icthh.xm.uaa.domain.User
import com.icthh.xm.uaa.service.UserService
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

class SystemQueueConsumerUnitTest extends AbstractGroovyUnitTest {

    String scriptName = LepTestConstants.LEP_DEFAULT_PATH + '/queue/system/AcceptSystemEvent$$UPDATE_ACCOUNT.groovy'
    String USER_KEY = "f81d3142-a259-4ff8-99e4-be533d68ca99";

    @Mock
    UserService userService

    @Override
    Object setLepContext() {
        MockitoAnnotations.initMocks(this)
        [
            inArgs  : [
                event: [
                    data: [
                        userKey: USER_KEY,
                        createdDate: "2017-11-20T13:15:30Z",
                        lastModifiedDate: "2023-09-01T23:40:21Z"
                    ] as Map
                ] as SystemEvent
            ],
            services: [
                userService: userService
            ]
        ]
    }

    private <T> T evaluateScript() {
        evaluateScript(scriptName)
    }

    @Test
    void updateProfile() {
        when(userService.getUser(USER_KEY)).thenReturn(new User())
        doNothing().when(userService).saveUser(any())

        evaluateScript()

        verify(userService).getUser(USER_KEY)
        verify(userService).saveUser(any())
    }

    @Test
    void updateNotExistsProfile() {
        when(userService.getUser(USER_KEY)).thenReturn(null);
        doNothing().when(userService).saveUser(any());

        evaluateScript()

        verify(userService).getUser(USER_KEY);
        verify(userService, times(0)).saveUser(any());
    }

}
