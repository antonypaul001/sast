package com.accenture.sast.tracker.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;

@Service
public class AuthService {

    // ── VULNERABILITY V3: Hardcoded Secret (CWE-798) ──────────────────────────
    // A secret embedded in source code is committed to version control and
    // visible to anyone with repository access.
    // Fix: load from an environment variable or a secrets manager, never from code.
    // ──────────────────────────────────────────────────────────────────────────

    private final AppUserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final String apiSecret;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${tracker.api-secret}") String apiSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiSecret = apiSecret;
    }

    // ── VULNERABILITY V2: Weak Cryptography (CWE-327) ─────────────────────────
    // MD5 is a broken hash function: it is fast to compute and has known collision
    // attacks. Passwords hashed with MD5 can be cracked from a 10-billion-entry
    // rainbow table in seconds.
    // Fix: use BCrypt, Argon2, or PBKDF2 — all supported by Spring Security's
    // PasswordEncoder interface.
    // ──────────────────────────────────────────────────────────────────────────
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public AppUser register(String username, String password) {
        String hash = hashPassword(password);
        return userRepository.save(new AppUser(username, hash));
    }

    public boolean authenticate(String username, String password) {
        Optional<AppUser> user = userRepository.findByUsername(username);
        if (user.isEmpty())
            return false;
        return passwordEncoder.matches(password, user.get().getPasswordHash());
    }

    public String getApiSecret() {
        return apiSecret;
    }
}
