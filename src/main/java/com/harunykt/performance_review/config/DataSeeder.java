package com.harunykt.performance_review.config;

import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(UserService userService) {
        return args -> {
            // Manager
            if (!userService.emailExists("manager@example.com")) {
                User m = new User();
                m.setFullName("Ali Yönetici");
                m.setEmail("manager@example.com");
                m.setPassword("123456"); // UserService encode edecek
                m.setRole(UserRole.MANAGER);
                userService.saveUser(m);
            }

            // Ayşe
            if (!userService.emailExists("ayse@example.com")) {
                User a = new User();
                a.setFullName("Ayşe Çalışkan");
                a.setEmail("ayse@example.com");
                a.setPassword("123456");
                a.setRole(UserRole.EMPLOYEE);
                userService.saveUser(a);
            }

            // Mehmet
            if (!userService.emailExists("mehmet@example.com")) {
                User meh = new User();
                meh.setFullName("Mehmet Üretken");
                meh.setEmail("mehmet@example.com");
                meh.setPassword("123456");
                meh.setRole(UserRole.EMPLOYEE);
                userService.saveUser(meh);
            }
        };
    }
}
