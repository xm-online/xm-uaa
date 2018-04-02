package com.icthh.xm.uaa.commons;

/**
 * The {@link XmRequestContextHolder} class.
 */
public interface XmRequestContextHolder {

    XmRequestContext getContext();

    XmPrivilegedRequestContext getPrivilegedContext();

}
