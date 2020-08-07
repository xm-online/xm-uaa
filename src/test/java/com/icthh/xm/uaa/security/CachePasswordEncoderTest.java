package com.icthh.xm.uaa.security;

import org.junit.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachePasswordEncoderTest {

    PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
    PasswordEncoder cachedEncoder = new CachePasswordEncoder(mockEncoder, 3);

    @Test
    public void ifPasswordWasNotCachedMatchWillBeCalled() {
        cachedEncoder.matches("rawPassword", "notCachedEncoder");
        verify(mockEncoder).matches(eq("rawPassword"), eq("notCachedEncoder"));
    }

    @Test
    public void ifPasswordWasRemovedCachedMatchWillBeCalled() {
        when(mockEncoder.matches(eq("rawPassword"), eq("notCachedEncoder"))).thenReturn(true);
        when(mockEncoder.matches(eq("rawPassword1"), eq("notCachedEncoder1"))).thenReturn(true);
        when(mockEncoder.matches(eq("rawPassword2"), eq("notCachedEncoder2"))).thenReturn(true);
        when(mockEncoder.matches(eq("rawPassword3"), eq("notCachedEncoder3"))).thenReturn(true);

        cachedEncoder.matches("rawPassword", "notCachedEncoder");
        cachedEncoder.matches("rawPassword1", "notCachedEncoder1");
        cachedEncoder.matches("rawPassword2", "notCachedEncoder2");
        cachedEncoder.matches("rawPassword3", "notCachedEncoder3");
        cachedEncoder.matches("rawPassword", "notCachedEncoder");

        verify(mockEncoder, times(2)).matches(eq("rawPassword"), eq("notCachedEncoder"));
    }

    @Test
    public void ifPasswordCachedMatchWillNotBeCalled() {
        when(mockEncoder.matches(eq("rawPassword"), eq("notCachedEncoder"))).thenReturn(true);
        cachedEncoder.matches("rawPassword", "notCachedEncoder");
        cachedEncoder.matches("rawPassword", "notCachedEncoder");
        verify(mockEncoder, times(1)).matches(eq("rawPassword"), eq("notCachedEncoder"));
    }

}
