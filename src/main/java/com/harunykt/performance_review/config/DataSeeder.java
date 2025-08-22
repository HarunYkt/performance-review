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
            User manager = null;
            if (!userService.emailExists("manager@example.com")) {
                User m = new User();
                m.setFullName("Ali Yönetici");
                m.setEmail("manager@example.com");
                m.setPassword("123456"); // UserService encode edecek
                m.setRole(UserRole.MANAGER);
                manager = userService.saveUser(m);
            } else {
                manager = userService.getByEmail("manager@example.com");
            }

            // Ayşe - Manager'a bağlı employee
            if (!userService.emailExists("ayse@example.com")) {
                User a = new User();
                a.setFullName("Ayşe Çalışkan");
                a.setEmail("ayse@example.com");
                a.setPassword("123456");
                a.setRole(UserRole.EMPLOYEE);
                a.setManager(manager); // Manager ilişkisi kuruldu
                userService.saveUser(a);
            }

            // Mehmet - Aynı manager'a bağlı employee (Ayşe ile peer olacak)
            if (!userService.emailExists("mehmet@example.com")) {
                User meh = new User();
                meh.setFullName("Mehmet Üretken");
                meh.setEmail("mehmet@example.com");
                meh.setPassword("123456");
                meh.setRole(UserRole.EMPLOYEE);
                meh.setManager(manager); // Aynı manager'a bağlı
                userService.saveUser(meh);
            }
        };
    }
}
