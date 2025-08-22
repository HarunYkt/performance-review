package com.harunykt.performance_review.controller;

import com.harunykt.performance_review.dto.LoginRequest;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.security.JwtService;
import com.harunykt.performance_review.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // JWT token ile login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Şifre hatalı"));
            }
            
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName(),
                            "role", user.getRole()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // GERÇEK: JWT üretir
    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Şifre hatalı"));
        }
        String accessToken = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }
}
