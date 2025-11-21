package com.icthh.xm.uaa.aop.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import java.util.ArrayList;
import java.util.List;

public class SecurityMaskingConsoleAppender extends ConsoleAppender<ILoggingEvent> {

    private final List<String> keywords = new ArrayList<>();
    private String replacementMessage;

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            keywords.add(keyword);
        }
    }

    public void setKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) return;

        for (String k : keywords.split(",")) {
            addKeyword(k.trim());
        }
    }

    public void setReplacementMessage(String replacementMessage) {
        if (replacementMessage != null && !replacementMessage.isBlank()) {
            this.replacementMessage = replacementMessage;
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        ILoggingEvent toLog = eventObject;

        String msg = eventObject.getFormattedMessage();
        if (msg == null) {
            msg = eventObject.getMessage();
        }

        if (msg != null && containsSensitive(msg)) {
            toLog = new MaskedLoggingEvent(eventObject, replacementMessage);
        }

        super.append(toLog);
    }

    private boolean containsSensitive(String msg) {
        if (keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (msg.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
