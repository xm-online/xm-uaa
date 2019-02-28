package com.icthh.xm.uaa.security;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DomainUserDetailsUnitTest {

    @Test
    public void testEquals() {
        DomainUserDetails details1 = new DomainUserDetails("admin", "password1", Collections.emptySet(), "xm", "key", false, null, null, false, null);
        DomainUserDetails details2 = new DomainUserDetails("admin", "password2", Collections.emptySet(), "xm", "key", false, null, null, false, null);

        assertEquals(details1, details2);
        assertEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    public void testNotEqualsClass() {
        DomainUserDetails details1 = new DomainUserDetails("admin", "password1", Collections.emptySet(), "xm", "key", false, null, null, false, null);

        assertNotEquals(details1, "admin");
    }

    @Test
    public void testNotEqualsUsername() {
        DomainUserDetails details1 = new DomainUserDetails("admin1", "password1", Collections.emptySet(), "xm", "key", false, null, null, false, null);
        DomainUserDetails details2 = new DomainUserDetails("admin2", "password2", Collections.emptySet(), "xm", "key", false, null, null, false, null);

        assertNotEquals(details1, details2);
    }


    @Test
    public void testNotEqualsTenant() {
        DomainUserDetails details1 = new DomainUserDetails("admin", "password1", Collections.emptySet(), "xm", "key", false, null, null, false, null);
        DomainUserDetails details2 = new DomainUserDetails("admin", "password2", Collections.emptySet(), "demo", "key", false, null, null, false, null);

        assertNotEquals(details1, details2);
    }
}
