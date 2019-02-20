package com.icthh.xm.uaa.commons;

/**
 * The {@link XmPrivilegedRequestContext} class.
 */
public interface XmPrivilegedRequestContext extends XmRequestContext {

    void putValue(String key, Object value);

    void destroyCurrentContext();

}
