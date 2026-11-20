package com.accenture.sast.tracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Supplier<UUID> uuidSupplier;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {

        final UUID uuid = uuidSupplier.get();

        log.error("Unhandled exception occurred, correlationId={}", uuid, ex);

        Map<String, String> body = Map.of(
                "error", "An unexpected error occurred",
                "correlationId", uuid.toString(),
                "message", "Please contact support with the correlation ID");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
