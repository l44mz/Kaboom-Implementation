package com.example.kaboom.services;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServices {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServices(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean register(String name, String email, String rawPassword) {
        if (name == null || !isValidEmail(email) || !isValidPassword(rawPassword)){
            return false;
        }
        if (userRepository.findByEmail(email).isPresent()){ //checks to see if email is already registered
            return false;
        }
       String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(name, email, hashedPassword);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean login(String email, String rawPassword) {
        if (isValidEmail(email) || isValidPassword(rawPassword)){
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash())).isPresent();
    }

    @Transactional
    public boolean update(UUID userId, String newName, String newEmail, String newRawPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        if (newEmail != null && !isValidEmail(newEmail)) return false;
        if (newRawPassword != null && !isValidPassword(newRawPassword)) return false;

        if (newName != null && !newName.trim().isEmpty()) user.setName(newName);
        if (newEmail != null) user.setEmail(newEmail);
        if (newRawPassword != null) user.setPasswordHash(passwordEncoder.encode(newRawPassword));

        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean delete(UUID userId) {
        if (!userRepository.existsById(userId)) return false;
        userRepository.deleteById(userId);
        return true;
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$");
    }

    private static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.matches(".*\\d.*");
    }
}
