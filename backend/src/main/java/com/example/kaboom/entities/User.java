package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private int coins;

    @Setter(AccessLevel.PROTECTED)
    @Column(name = "role")
    private String role = "user";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public User(UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public boolean register(String name, String email, String rawPassword) {
        if (name == null || !isValidEmail(email) || !isValidPassword(rawPassword)){
            return false;
        }
        this.name = name;
        this.email = email;
        this.passwordHash = ENCODER.encode(rawPassword);
        this.coins = 0;
        this.role = "user";
        this.createdAt = LocalDateTime.now();
        return true;
    }

    public boolean login(String email, String rawPassword) {
        return isValidEmail(email) && isValidPassword(rawPassword) && ENCODER.matches(rawPassword, this.passwordHash);
    }

    public boolean update(String newName, String newEmail, String newRawPassword) {
        if (newEmail != null && !isValidEmail(newEmail)) return false;
        if (newRawPassword != null && !isValidPassword(newRawPassword)) return false;

        if (newName != null && !newName.trim().isEmpty()) name = newName;
        if (newEmail != null) email = newEmail;
        if (newRawPassword != null) passwordHash = ENCODER.encode(newRawPassword);
        return true;
    }

    public boolean delete(){
        name = null;
        email = null;
        passwordHash = null;
        coins = 0;
        return true;
    }

    public void addReward(int amount){
        if (amount <= 0) throw new IllegalArgumentException("Amount cannot be negative!");
        coins += amount;
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$");
    }

    private static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && password.matches(".*\\d.*");
    }
}