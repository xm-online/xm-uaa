package com.icthh.xm.uaa.security.ldap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@RunWith(MockitoJUnitRunner.class)
public class CutDomainAuthenticationProviderDecoratorUnitTest {

    @InjectMocks
    CutDomainAuthenticationProviderDecorator authenticationProviderDecorator;

    @Mock
    AuthenticationProvider authenticationProvider;

    @Mock
    TenantProperties.Ldap conf;

    @Test
    public void splitDomainWhenItsEnabled() {
        when(conf.getUseNameWithoutDomain()).thenReturn(true);
        authenticationProviderDecorator.authenticate(new UsernamePasswordAuthenticationToken("name@domain", "password"));
        verify(authenticationProvider).authenticate(new UsernamePasswordAuthenticationToken("name", "password"));
    }

    @Test
    public void checkPosibilityToUseEmailWithLoginByAd() {
        when(conf.getUseNameWithoutDomain()).thenReturn(true);
        authenticationProviderDecorator.authenticate(new UsernamePasswordAuthenticationToken("name@mail.com@domain", "password"));
        verify(authenticationProvider).authenticate(new UsernamePasswordAuthenticationToken("name@mail.com", "password"));
    }

    @Test
    public void noErrosWhenLoginWithoutDomain() {
        when(conf.getUseNameWithoutDomain()).thenReturn(true);
        authenticationProviderDecorator.authenticate(new UsernamePasswordAuthenticationToken("name", "password"));
        verify(authenticationProvider).authenticate(new UsernamePasswordAuthenticationToken("name", "password"));
    }

    @Test
    public void doNotsplitDomainWhenItsDisabled() {
        when(conf.getUseNameWithoutDomain()).thenReturn(false);
        authenticationProviderDecorator.authenticate(new UsernamePasswordAuthenticationToken("name@domain", "password"));
        verify(authenticationProvider).authenticate(new UsernamePasswordAuthenticationToken("name@domain", "password"));
    }

}
