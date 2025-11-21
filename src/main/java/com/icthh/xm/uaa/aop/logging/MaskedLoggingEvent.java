package com.icthh.xm.uaa.aop.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import java.util.Map;
import org.slf4j.Marker;

public class MaskedLoggingEvent implements ILoggingEvent {

    private final ILoggingEvent delegate;
    private final String maskedMessage;

    public MaskedLoggingEvent(ILoggingEvent delegate, String maskedMessage) {
        this.delegate = delegate;
        this.maskedMessage = maskedMessage;
    }

    @Override
    public String getThreadName() {
        return delegate.getThreadName();
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public String getMessage() {
        return maskedMessage;
    }

    @Override
    public Object[] getArgumentArray() {
        return null;
    }

    @Override
    public String getFormattedMessage() {
        return maskedMessage;
    }

    @Override
    public String getLoggerName() {
        return delegate.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return delegate.getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return delegate.getThrowableProxy();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return delegate.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return delegate.hasCallerData();
    }

    @Override
    public Marker getMarker() {
        return delegate.getMarker();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return delegate.getMDCPropertyMap();
    }

    @Override
    public Map<String, String> getMdc() {
        return delegate.getMdc();
    }

    @Override
    public long getTimeStamp() {
        return delegate.getTimeStamp();
    }

    @Override
    public void prepareForDeferredProcessing() {
        delegate.prepareForDeferredProcessing();
    }
}
