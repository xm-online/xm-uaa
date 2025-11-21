package com.icthh.xm.uaa.aop.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;

class SecurityMaskingConsoleAppenderUnitTest {

    @Test
    void shouldReplaceMessageForPatternEncoderWhenContainsKeyword() {
        LoggerContext context = new LoggerContext();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            // encoder
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern("%msg%n");
            encoder.start();

            // appender
            SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
            appender.setContext(context);
            appender.setEncoder(encoder);
            appender.setKeywords("keyword:, keyword=");
            appender.setReplacementMessage("this log was removed due to potential security breach");
            appender.start();

            // logger
            Logger logger = context.getLogger("test.pattern.logger");
            logger.detachAndStopAllAppenders();
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
            logger.addAppender(appender);

            // when
            logger.info("safe message");
            logger.info("keyword: 1234");

            System.out.flush();

        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        String[] lines = output.split("\\R");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo("safe message");
        assertThat(lines[1]).isEqualTo("this log was removed due to potential security breach");
    }

    @Test
    void shouldReplaceMessageForJsonEncoderWhenContainsKeyword() {
        LoggerContext context = new LoggerContext();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            // JSON encoder
            LogstashEncoder encoder = new LogstashEncoder();
            encoder.setContext(context);
            encoder.start();

            SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
            appender.setContext(context);
            appender.setEncoder(encoder);
            appender.setKeywords("keyword:, keyword=");
            appender.setReplacementMessage("this log was removed due to potential security breach");
            appender.start();

            Logger logger = context.getLogger("test.json.logger");
            logger.detachAndStopAllAppenders();
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
            logger.addAppender(appender);

            // when
            logger.info("safe json");
            logger.info("keyword=qwerty");

            System.out.flush();
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        String[] lines = output.split("\\R");

        assertThat(lines.length).isGreaterThanOrEqualTo(2);

        String first = lines[0];
        String second = lines[1];

        assertThat(first).contains("safe json");
        assertThat(first).doesNotContain("this log was removed due to potential security breach");

        assertThat(second).contains("this log was removed due to potential security breach");
        assertThat(second).doesNotContain("keyword=qwerty");
    }

    @Test
    void shouldNotMaskWhenNoKeywordsConfigured() {
        LoggerContext context = new LoggerContext();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern("%msg%n");
            encoder.start();

            SecurityMaskingConsoleAppender appender = new SecurityMaskingConsoleAppender();
            appender.setContext(context);
            appender.setEncoder(encoder);
            appender.start();

            Logger logger = context.getLogger("test.no.keyword");
            logger.detachAndStopAllAppenders();
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
            logger.addAppender(appender);

            logger.info("keyword: should stay as is");

            System.out.flush();
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString().trim();

        assertThat(output).isEqualTo("keyword: should stay as is");
    }
}
