package com.icthh.xm.uaa.commons.base;

import com.icthh.xm.uaa.commons.XmPrivilegedRequestContext;

import java.util.Objects;

/**
 * The {@link BaseXmPrivilegedRequestContext} class.
 */
class BaseXmPrivilegedRequestContext implements XmPrivilegedRequestContext {

    private final XmRequestContextData data;

    BaseXmPrivilegedRequestContext(XmRequestContextData data) {
        this.data = Objects.requireNonNull(data, "data can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putValue(String key, Object value) {
        data.putValue(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyCurrentContext() {
        data.destroyCurrent();
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
