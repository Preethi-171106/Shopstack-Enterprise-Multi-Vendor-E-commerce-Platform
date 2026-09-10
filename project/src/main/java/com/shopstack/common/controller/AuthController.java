package com.shopstack.common.controller;

import com.shopstack.common.dto.AuthResponse;
import com.shopstack.common.dto.LoginRequest;
import com.shopstack.common.entity.User;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.repository.UserRepository;
import com.shopstack.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }
        ArrayList<String> roles = new ArrayList<>(user.getRoles());
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), roles);
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail());
    }
}
