package com.icthh.xm.ms.entity.logging;

import com.icthh.xm.commons.logging.util.MDCUtil;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for MDCUtil class.
 */
public class MDCUtilTest {

    private static final int RID_LEN = 8;

    @Before
    public void before() {
        MDCUtil.clear();
    }

    @Test
    public void testGenerateRid() {
        assertNotNull(MDCUtil.generateRid());
        assertEquals(MDCUtil.generateRid().length(), RID_LEN);
        assertTrue(StringUtils.isAsciiPrintable(MDCUtil.generateRid()));
    }

    @Test
    public void testGetTime() {
        assertTrue(MDCUtil.getTime() == 0);
        MDCUtil.put("key", "value");
        assertTrue(MDCUtil.getExecTime() > 0);
    }

    @Test
    public void testPutRid() {
        assertNull(MDCUtil.getRid());
        MDCUtil.put("key", "value");
        assertEquals("value", MDC.get("key"));

        assertNull(MDCUtil.getRid());

        MDCUtil.putRid("myRid");
        assertEquals("myRid", MDCUtil.getRid());
        assertEquals("myRid", MDC.get("rid"));

        MDCUtil.clear();

        assertNull(MDCUtil.getRid());
        assertNull(MDC.get("key"));
    }

}
