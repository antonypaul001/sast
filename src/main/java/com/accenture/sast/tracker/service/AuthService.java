package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // ── VULNERABILITY V3: Hardcoded Secret (CWE-798) ──────────────────────────
    // A secret embedded in source code is committed to version control and
    // visible to anyone with repository access.
    // Fix: load from an environment variable or a secrets manager, never from code.
    // ──────────────────────────────────────────────────────────────────────────

    private final AppUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String apiSecret;

    public AuthService(
            AppUserRepository userRepository,
            @Value("${API_SECRET}") String apiSecret) {
        this.userRepository = userRepository;
        this.apiSecret = apiSecret;
    }

    // ── VULNERABILITY V2: Weak Cryptography (CWE-327) ─────────────────────────
    // MD5 is a broken hash function: it is fast to compute and has known collision
    // attacks.  Passwords hashed with MD5 can be cracked from a 10-billion-entry
    // rainbow table in seconds.
    // Fix: use BCrypt, Argon2, or PBKDF2 — all supported by Spring Security's
    //      PasswordEncoder interface.
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
        if (user.isEmpty()) return false;
        return passwordEncoder.matches(password, user.get().getPasswordHash());
    }

    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }   

    public String getApiSecret() {
        return apiSecret;
    }
    public Optional<AppUser> findByUsername(String username) {
    return userRepository.findByUsername(username);
}
}
