package com.harunykt.performance_review.controller;

import com.harunykt.performance_review.dto.LoginRequest;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        return userService.findByEmail(request.getEmail())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return ResponseEntity.ok("Giriş başarılı. Hoş geldin, " + user.getFullName() + "!");
                    } else {
                        return ResponseEntity.badRequest().body("Şifre hatalı.");
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Kullanıcı bulunamadı."));
    }
}
