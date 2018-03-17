package com.icthh.xm.uaa.commons.base;

import com.icthh.xm.uaa.commons.XmPrivilegedRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContext;
import com.icthh.xm.uaa.commons.XmRequestContextHolder;

/**
 * The {@link DefaultXmRequestContextHolder} class.
 */
public class DefaultXmRequestContextHolder implements XmRequestContextHolder {

    /**
     * {@inheritDoc}
     */
    @Override
    public XmRequestContext getContext() {
        return new BaseXmRequestContext(XmRequestContextData.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmPrivilegedRequestContext getPrivilegedContext() {
        return new BaseXmPrivilegedRequestContext(XmRequestContextData.get());
    }

}
