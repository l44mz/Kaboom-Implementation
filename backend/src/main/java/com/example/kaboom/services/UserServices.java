package com.example.kaboom.services;

import com.example.kaboom.dtos.*;
import com.example.kaboom.entities.User;
import com.example.kaboom.mappers.UserMapper;
import com.example.kaboom.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServices {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServices jwtServices;
    private final UserMapper userMapper;

    public UserServices(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtServices jwtServices, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtServices = jwtServices;
        this.userMapper = userMapper;
    }

    @Transactional
    public RegisterResponseDto register(RegisterDto dto) {
        String email = dto.getEmail() == null ? null : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        if (dto.getName() == null || dto.getName().isBlank()
                || !isValidEmail(email) || !isValidPassword(dto.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name, email or password");
        }
        if (userRepository.findByEmail(email).isPresent()) { // email already registered
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(dto.getName().trim(), email, passwordEncoder.encode(dto.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new RegisterResponseDto("User registered successfully", userMapper.toDto(user));
    }

    public LoginResponseDto login(LoginDto dto) {
        String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(dto.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new LoginResponseDto(
                jwtServices.generateToken(user.getEmail(), user.getRole()),
                jwtServices.generateRefreshToken(user.getEmail()),
                jwtServices.getAccessExpirationSeconds(),
                userMapper.toDto(user));
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

    public RefreshResponseDto refresh(RefreshDto dto) {
        String token = dto.getRefreshToken();

        if (!jwtServices.isValidRefreshToken(token)) {   
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        String email = jwtServices.extractEmail(token);  
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"));

        return new RefreshResponseDto(
                jwtServices.generateToken(user.getEmail(), user.getRole()),
                jwtServices.getAccessExpirationSeconds());
    }

    public MessageResponseDto logout(LogoutDto dto) {
        return new MessageResponseDto("Logged out");
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
