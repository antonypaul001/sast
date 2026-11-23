package com.accenture.sast.tracker.service;

import com.accenture.sast.tracker.model.AppUser;
import com.accenture.sast.tracker.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (user.isEmpty()) return false;
        return passwordEncoder.matches(password, user.get().getPasswordHash());
    }

    @Transactional(readOnly = true)
    public ProfileAccess profileAccess(String username, Long employeeId) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (user.isHr()) return ProfileAccess.HR;
                    if (user.getEmployee() != null && employeeId.equals(user.getEmployee().getId())) {
                        return ProfileAccess.OWN;
                    }
                    return ProfileAccess.DENIED;
                })
                .orElse(ProfileAccess.DENIED);
    }

    public enum ProfileAccess { DENIED, OWN, HR }

}
