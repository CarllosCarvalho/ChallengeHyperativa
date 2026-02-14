package com.hyperativa.cardapi.service;

import com.hyperativa.cardapi.dto.AuthRequest;
import com.hyperativa.cardapi.dto.AuthResponse;
import com.hyperativa.cardapi.entity.User;
import com.hyperativa.cardapi.repository.UserRepository;
import com.hyperativa.cardapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates the user and returns a JWT token.
     */
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.getActive()) {
            throw new BadCredentialsException("Inactive user");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        log.info("User '{}' authenticated successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

    /**
     * Registers a new user (for testing convenience).
     */
    public void register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        log.info("User '{}' registered successfully", request.getUsername());
    }
}
