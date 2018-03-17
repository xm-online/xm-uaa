package com.icthh.xm.uaa.commons;

/**
 * The {@link XmRequestContext} interface.
 */
public interface XmRequestContext {

    boolean containsKey(String key);

    <T> T getValue(String key, Class<T> type);

    <T> T getValue(String key, T defaultValue, Class<T> type);

}
