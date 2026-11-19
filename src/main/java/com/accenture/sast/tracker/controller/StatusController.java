package com.accenture.sast.tracker.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

// ── VULNERABILITY V8: Reflected Cross-Site Scripting (CWE-79) ────────────────
// The message parameter is embedded directly into the HTML response body without
// any output encoding. An attacker can inject a script payload:
//   GET /api/status?message=<script>alert(document.cookie)</script>
// and the browser executes it in the context of the application's origin,
// enabling session hijacking, credential theft, or malicious redirects.
// Fix: encode the parameter before embedding it, e.g.:
//   org.springframework.web.util.HtmlUtils.htmlEscape(message)
// and add a Content-Security-Policy response header to restrict script execution.
// ─────────────────────────────────────────────────────────────────────────────
@RestController
public class StatusController {

    static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'self'; img-src 'self'; base-uri 'none'; frame-ancestors 'none'";

    @GetMapping(value = "/api/status", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> status(@RequestParam(defaultValue = "Welcome") String message) {
        String escapedMessage = HtmlUtils.htmlEscape(message);

        String html = "<html><body><h1>" + escapedMessage + "</h1>" +
                "<p>Employee Tracker is running.</p></body></html>";

        return ResponseEntity.ok()
                .header(HEADER_CONTENT_SECURITY_POLICY,
                        CONTENT_SECURITY_POLICY)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
