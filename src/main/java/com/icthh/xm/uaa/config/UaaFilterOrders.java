package com.icthh.xm.uaa.config;

import lombok.experimental.UtilityClass;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@UtilityClass
public class UaaFilterOrders {

    public static final int LEP_THREAD_CONTEXT_FILTER_ORDER = HIGHEST_PRECEDENCE;
    public static final int XM_CONTEXT_FILTER_ORDER = HIGHEST_PRECEDENCE + 1;
    public static final int CUSTOM_AUTHENTICATION_FILTER_ORDER = HIGHEST_PRECEDENCE + 2;
    public static final int LOGGING_FILTER_ORDER = HIGHEST_PRECEDENCE + 3;

}
