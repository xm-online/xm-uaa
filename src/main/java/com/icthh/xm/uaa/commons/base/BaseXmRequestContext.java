package com.icthh.xm.uaa.commons.base;

import com.icthh.xm.uaa.commons.XmRequestContext;

import java.util.Objects;

/**
 * The {@link BaseXmRequestContext} class.
 */
class BaseXmRequestContext implements XmRequestContext {

    private final XmRequestContextData data;

    BaseXmRequestContext(XmRequestContextData data) {
        this.data = Objects.requireNonNull(data, "data can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValue(String key, Class<T> type) {
        return data.getValue(key, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValue(String key, T defaultValue, Class<T> type) {
        return data.getValue(key, defaultValue, type);
    }

}
