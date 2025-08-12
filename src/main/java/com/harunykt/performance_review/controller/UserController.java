package com.harunykt.performance_review.controller;

import com.harunykt.performance_review.dto.UserDto;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController (UserService userService) {
        this.userService = userService;
    }

    // ✅ Yeni kullanıcı oluşturma (register)
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.badRequest().body("Bu e-posta zaten kayıtlı");
        }
        user.setRole(UserRole.EMPLOYEE);
        userService.saveUser(user);
        return ResponseEntity.ok("Kullanıcı Başarıyla Kaydedildi");
    }

    // ✅ E-posta ile kullanıcı bulma (DTO döner, şifre sızmaz)
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email)
                .map(u -> ResponseEntity.ok(userService.toDto(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Yalnızca MANAGER tüm kullanıcıları listeleyebilir (DTO listesi)
    @GetMapping
    public ResponseEntity<?> listAllUsers(Authentication authentication) {
        User requester = userService.findByEmail(authentication.getName()).orElseThrow();
        if (requester.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(403).body("Bu endpoint'e sadece yöneticiler erişebilir.");
        }
        List<UserDto> list = userService.findAll()
                .stream()
                .map(userService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ✅ Giriş yapan kişi kendi profilini görür
    @GetMapping("/me")
    public ResponseEntity<?> myProfile(Authentication authentication) {
        User me = userService.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(userService.toDto(me));
    }

    // YENİ: Manager atama (sadece MANAGER olan kişi kullanabilsin)
    @PutMapping("/{userId}/manager/{managerId}")
    public ResponseEntity<?> assignManager(@PathVariable Long userId,
                                           @PathVariable Long managerId,
                                           Authentication auth) {
        User current = userService.getByEmail(auth.getName()); // auth principal e-posta
        if (current.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(403).body("Sadece yöneticiler atama yapabilir");
        }
        try {
            userService.assignManager(userId, managerId);
            return ResponseEntity.ok("Yönetici atandı");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ✅ Kendi adını güncelleme örneği
    public static class UpdateNameRequest {
        private String fullName;
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyName(@RequestBody UpdateNameRequest req, Authentication authentication) {
        if (req.getFullName() == null || req.getFullName().isBlank()) {
            return ResponseEntity.badRequest().body("fullName boş olamaz.");
        }
        User me = userService.findByEmail(authentication.getName()).orElseThrow();
        me.setFullName(req.getFullName());
        return ResponseEntity.ok(userService.toDto(userService.saveUser(me)));
    }
}
