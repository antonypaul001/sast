package com.accenture.sast.tracker.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

@RestController
public class StatusController {

    @GetMapping(value = "/api/status", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> status(@RequestParam(defaultValue = "Welcome") String message) {
        final String body = "<html><body><h1>%s</h1><p>Employee Tracker is running.</p></body></html>";
        return ResponseEntity.ok()
                .header("Content-Security-Policy", "script-src 'self'")
                .body(body.formatted(htmlEscape(message)));
    }

}
