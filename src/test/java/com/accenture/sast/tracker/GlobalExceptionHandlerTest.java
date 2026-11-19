package com.accenture.sast.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.accenture.sast.tracker.controller.GlobalExceptionHandler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class GlobalExceptionHandlerTest {

    private Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void handleAll_returnsGenericErrorMessageWithCorrelationId() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleAll(new IllegalArgumentException("Invalid input"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("error"));
        assertNotNull(response.getBody().get("correlationId"));
    }

    @Test
    void handleAll_makesCorrectServerLogEntry() {
        String expectedMessage = "Unhandled server exception [correlationId=";

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        handler.handleAll(new RuntimeException("Test exception"));

        assertEquals(1, appender.list.size());

        assertTrue(appender.list.get(0).getFormattedMessage().startsWith(expectedMessage));

        assertEquals("Test exception",
                appender.list.get(0).getThrowableProxy()
                        .getMessage());
    }
}
