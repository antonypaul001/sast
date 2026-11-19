package com.accenture.sast.tracker.controller;

import com.accenture.sast.tracker.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        authService.register(username, password);
        return ResponseEntity.ok(Map.of("message", "User registered"));
    }

    // ── VULNERABILITY V4: Sensitive Data in Logs (CWE-532) ───────────────────
    // The plaintext password is written to the application log.
    // Log files are often forwarded to centralised systems (Splunk, ELK) where
    // they can be read by many people and stored indefinitely.
    // Fix: log only the username; never log credentials or tokens.
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");

        log.info("Login attempt: username={}", username);

        if (authService.authenticate(username, password)) {
            setSession(username, request, response);
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }
        return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
    }

    // Stores the authenticated identity in the HttpSession (JSESSIONID cookie in the client side).
    private void setSession(String username, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(username, null, List.of());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);

        if (request.getSession(false) != null) {
            request.changeSessionId(); // prevent session fixation
        }
        new HttpSessionSecurityContextRepository().saveContext(ctx, request, response);
        request.getSession().setAttribute("userId", authService.findUserId(username));
    }
}
