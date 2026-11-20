package com.accenture.sast.tracker.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.function.Supplier;

import static com.accenture.sast.tracker.controller.GlobalExceptionHandlerTest.FailingController;
import static com.accenture.sast.tracker.controller.GlobalExceptionHandlerTest.TestConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(FailingController.class)
@Import({GlobalExceptionHandler.class, TestConfig.class})
class GlobalExceptionHandlerTest {

    private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsCorrelationIdForUnhandledException(final CapturedOutput output) throws Exception {
        mvc.perform(get("/test/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.correlationId").value(ID.toString()))
                .andExpect(jsonPath("$.message").value("Please contact support with the correlation ID"));

        assertThat(output.getOut(), containsString("Unhandled exception occurred, correlationId=" + ID));
    }

    @RestController
    static class FailingController {
        @GetMapping("/test/fail")
        void fail() {
            throw new IllegalStateException("test failure");
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        Supplier<UUID> uuidSupplier() {
            return () -> ID;
        }
    }
}