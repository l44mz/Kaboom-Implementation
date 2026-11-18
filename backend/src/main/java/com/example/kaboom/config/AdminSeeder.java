
package com.example.kaboom.config;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;

@Configuration
public class AdminSeeder {

    private static final Logger log =
            LoggerFactory.getLogger(AdminSeeder.class);

    @Value("${kaboom.admin.name:Lab Admin}")
    private String adminName;

    @Value("${kaboom.admin.email:}")
    private String adminEmail;

    @Value("${kaboom.admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                log.warn("Admin seeding skipped: configure admin email and password.");
                return;
            }

            String email = adminEmail.trim().toLowerCase(Locale.ROOT);

            var existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                if (!"admin".equalsIgnoreCase(user.getRole())) {
                    throw new IllegalStateException(
                            "The configured admin email already belongs to a non-admin user."
                    );
                }

                log.info("Seeded admin already exists.");
                return;
            }

            User admin = new User(
                    adminName.trim(),
                    email,
                    passwordEncoder.encode(adminPassword),
                    "admin"
            );

            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);

            log.info("Seeded admin account created successfully.");
        };
    }
}
