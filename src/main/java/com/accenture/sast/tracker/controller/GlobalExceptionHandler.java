package com.accenture.sast.tracker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
// ── VULNERABILITY V6: Verbose Error Disclosure (CWE-209) ─────────────────────
// This handler catches every unhandled exception and returns the exception class
// name and message directly in the HTTP response body.
// An attacker can craft requests that trigger exceptions (e.g. malformed input,
// boundary conditions) and read the diagnostic detail to discover internal paths,
// database schema names, or class names that enable further exploitation.
// Fix: log the exception server-side with a correlation ID and return only a
// generic message to the client, e.g. { "error": "An unexpected error occurred" }.
// ─────────────────────────────────────────────────────────────────────────────
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {

        log.error("Unhandled exception occurred", ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "An unexpected error occurred"
            ));
    }
}
