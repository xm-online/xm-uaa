package com.icthh.xm.uaa.security;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CustomizableLepTokenStorageUnitTest {
    @Mock(answer = Answers.RETURNS_MOCKS)
    JwtTokenStore jwtTokenStore;
    @InjectMocks
    CustomizableLepTokenStorageImpl customizableLepTokenStorage;

    @Test
    public void testReadRefreshToken() {
        OAuth2RefreshToken result = customizableLepTokenStorage.readRefreshToken("tokenValue");
        assertNotNull(result);
        verify(jwtTokenStore).readRefreshToken("tokenValue");
    }
}
