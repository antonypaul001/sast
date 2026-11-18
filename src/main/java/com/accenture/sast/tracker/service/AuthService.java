package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    // ── VULNERABILITY V3: Hardcoded Secret (CWE-798) ──────────────────────────
    // A secret embedded in source code is committed to version control and
    // visible to anyone with repository access.
    // Fix: load from an environment variable or a secrets manager, never from code.
    // ──────────────────────────────────────────────────────────────────────────

    private final AppUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String apiSecret;

    public AuthService(AppUserRepository userRepository,
            @Value("${app.api-secret}") String apiSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.apiSecret = apiSecret;
    }

    // ── VULNERABILITY V2: Weak Cryptography (CWE-327) ─────────────────────────
    // MD5 is a broken hash function: it is fast to compute and has known collision
    // attacks.  Passwords hashed with MD5 can be cracked from a 10-billion-entry
    // rainbow table in seconds.
    // Fix: use BCrypt, Argon2, or PBKDF2 — all supported by Spring Security's
    //      PasswordEncoder interface.
    // ─────────────────────────────────────────────────────────────────────────

        public AppUser register(String username, String password) {
            String hash = passwordEncoder.encode(password);
            return userRepository.save(new AppUser(username, hash));
        }

        public boolean authenticate(String username, String password) {
            Optional<AppUser> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                return false;
            //return user.get().getPasswordHash().equals(hashPassword(password));           
            }
            return passwordEncoder.matches(password, user.get().getPasswordHash());
        }

        public String getApiSecret() {
            return apiSecret;
        }
}
