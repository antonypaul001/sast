package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
public class AuthService {

    private final String apiSecret;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${API_SECRET}") String apiSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiSecret = apiSecret;
    }

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

    public Long findUserIdByName(String username) {
        Optional<AppUser> user = userRepository.findByUsername(username);
        return user.map(AppUser::getId).orElse(null);
    }

    public String getApiSecret() {
        return apiSecret;
    }
}
