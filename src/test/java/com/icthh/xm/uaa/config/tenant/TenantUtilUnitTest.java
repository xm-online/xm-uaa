package com.icthh.xm.uaa.config.tenant;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TenantUtilUnitTest {

    @Test
    public void testGetApplicationUrl() throws Exception {
        TenantContext.setCurrent(new TenantInfo("", "", "","http", "localhost", "8080", ""));

        assertEquals("http://localhost:8080", TenantUtil.getApplicationUrl());

        TenantContext.setCurrent(new TenantInfo("", "", "","http", "localhost", "80", ""));

        assertEquals("http://localhost", TenantUtil.getApplicationUrl());
    }
}
