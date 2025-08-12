package com.harunykt.performance_review.service;

import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Email ile kullanıcı getir
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı Bulunamadı" + email));
    }

    // --- YENİ: id ile kullanıcı getir ---
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı: " + id));
    }

    // ⬇️ Controller'ın aradığı metot
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {

        return userRepository.findById(id);
    }

    public boolean emailExists(String email) {

        return userRepository.existsByEmail(email);
    }

    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Profil güncelleme
    public User updateFullName (User user, String newFullName) {
        user.setFullName(newFullName);
        return userRepository.save(user);
    }

    // Dışarıya UserDto üretmek için yardımcı (opsiyonel)
    public com.harunykt.performance_review.dto.UserDto toDto(User u) {
        return new com.harunykt.performance_review.dto.UserDto(
                u.getId(), u.getFullName(), u.getEmail(), u.getRole().name()
        );
    }

    public List<com.harunykt.performance_review.dto.UserDto> toDtoList(List<User> users) {
     return users.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ...
    @Transactional
    public void assignManager(Long userId, Long managerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Yönetici bulunamadı"));

        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Atanan kişi MANAGER olmalı");
        }
        if (user.getId().equals(manager.getId())) {
            throw new IllegalArgumentException("Kişi kendisinin yöneticisi olamaz");
        }
        user.setManager(manager);
        userRepository.save(user);
    }





}
