package com.icthh.xm.uaa.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.ansi.AnsiColor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CRLFLogConverterUnitTest {

    private static final String TEST_INPUT_STRING = "Test input string";
    private static final String TEST_INPUT_STRING_TAG = "Test\ninput\rstring";

    @Test
    @DisplayName("Returns input when marker list is null and logger is safe")
    void transform_nullMarkers_returnsInput() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(null);
        when(event.getLoggerName()).thenReturn("org.hibernate.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        String result = converter.transform(event, TEST_INPUT_STRING);

        assertEquals(TEST_INPUT_STRING, result);
    }

    @Test
    @DisplayName("Returns input when markers contain CRLF_SAFE marker")
    void transform_crlfSafeMarker_returnsInput() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        Marker marker = MarkerFactory.getMarker("CRLF_SAFE");
        List<Marker> markers = Collections.singletonList(marker);
        when(event.getMarkerList()).thenReturn(markers);
        when(event.getLoggerName()).thenReturn("org.hibernate.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        String result = converter.transform(event, TEST_INPUT_STRING);

        assertEquals(TEST_INPUT_STRING, result);
    }

    @Test
    @DisplayName("Returns input when markers do not contain CRLF_SAFE and logger is safe")
    void transform_unsafeMarkerSafeLogger_returnsInput() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        Marker marker = MarkerFactory.getMarker("CRLF_NOT_SAFE");
        List<Marker> markers = Collections.singletonList(marker);
        when(event.getMarkerList()).thenReturn(markers);
        when(event.getLoggerName()).thenReturn("org.hibernate.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        String result = converter.transform(event, TEST_INPUT_STRING);

        assertEquals(TEST_INPUT_STRING, result);
    }

    @Test
    @DisplayName("Returns input when logger name is in safe list")
    void transform_safeLogger_returnsInput() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("org.hibernate.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        String result = converter.transform(event, TEST_INPUT_STRING);

        assertEquals(TEST_INPUT_STRING, result);
    }

    @Test
    @DisplayName("Replaces CR/LF with underscore when logger is unsafe and no safe marker")
    void transform_unsafeLoggerNoSafeMarker_replacesNewlines() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(Collections.emptyList());
        when(event.getLoggerName()).thenReturn("com.mycompany.myapp.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        String result = converter.transform(event, TEST_INPUT_STRING_TAG);

        assertEquals("Test_input_string", result);
    }

    @Test
    @DisplayName("Replaces CR/LF with underscore even when ANSI element is set")
    void transform_withAnsiOption_replacesNewlines() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMarkerList()).thenReturn(Collections.emptyList());
        when(event.getLoggerName()).thenReturn("com.mycompany.myapp.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();
        converter.setOptionList(List.of("red"));

        String result = converter.transform(event, TEST_INPUT_STRING_TAG);

        assertEquals("Test_input_string", result);
    }

    @Test
    @DisplayName("Returns true when logger name starts with a known safe prefix")
    void isLoggerSafe_safePrefix_returnsTrue() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("org.springframework.boot.autoconfigure.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        assertTrue(converter.isLoggerSafe(event));
    }

    @Test
    @DisplayName("Returns false when logger name does not match any safe prefix")
    void isLoggerSafe_unknownPrefix_returnsFalse() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("com.mycompany.myapp.example.Logger");
        CRLFLogConverter converter = new CRLFLogConverter();

        assertFalse(converter.isLoggerSafe(event));
    }

    @Test
    @DisplayName("Returns plain string when ANSI element is applied (ANSI disabled in test env)")
    void toAnsiString_withAnsiElement_returnsPlainString() {
        CRLFLogConverter cut = new CRLFLogConverter();

        String result = cut.toAnsiString("input", AnsiColor.RED);

        assertThat(result).isEqualTo("input");
    }
}
