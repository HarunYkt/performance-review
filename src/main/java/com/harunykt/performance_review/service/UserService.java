package com.harunykt.performance_review.service;

import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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




}
