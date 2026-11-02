
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

    public UserServices(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtServices jwtServices,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtServices = jwtServices;
        this.userMapper = userMapper;
    }

    @Transactional
    public RegisterResponseDto register(RegisterDto dto) {
        String email = dto.getEmail() == null
                ? null
                : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        if (dto.getName() == null || dto.getName().isBlank()
                || !isValidEmail(email)
                || !isValidPassword(dto.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid name, email or password"
            );
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        User user = new User(
                dto.getName().trim(),
                email,
                passwordEncoder.encode(dto.getPassword())
        );

        // Give every newly registered user 100 coins as a starting balance.
        user.setCoins(100);

        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return new RegisterResponseDto(
                "User registered successfully",
                userMapper.toDto(user)
        );
    }

    public LoginResponseDto login(LoginDto dto) {
        String email = dto.getEmail() == null
                ? ""
                : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .filter(u -> dto.getPassword() != null
                        && passwordEncoder.matches(
                                dto.getPassword(),
                                u.getPasswordHash()
                        ))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        return new LoginResponseDto(
                jwtServices.generateToken(user.getEmail(), user.getRole()),
                jwtServices.generateRefreshToken(user.getEmail()),
                "Bearer",
                jwtServices.getAccessExpirationSeconds(),
                userMapper.toDto(user)
        );
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    @Transactional
    public UserDto updateUser(UUID userId, UserDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name cannot be empty"
            );
        }

        String email = dto.getEmail() == null
                ? null
                : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!isValidEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email"
            );
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()
                && !existingUser.get().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already in use"
            );
        }

        user.setName(dto.getName().trim());
        user.setEmail(email);

        userRepository.save(user);

        return userMapper.toDto(user);
    }

    @Transactional
    public boolean update(
            UUID userId,
            String newName,
            String newEmail,
            String newRawPassword) {

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (newEmail != null) {
            String email = newEmail.trim().toLowerCase(Locale.ROOT);

            if (!isValidEmail(email)) {
                return false;
            }

            Optional<User> existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()
                    && !existingUser.get().getId().equals(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email already in use"
                );
            }

            user.setEmail(email);
        }

        if (newRawPassword != null && !isValidPassword(newRawPassword)) {
            return false;
        }

        if (newName != null && !newName.isBlank()) {
            user.setName(newName.trim());
        }

        if (newRawPassword != null) {
            user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        }

        userRepository.save(user);
        return true;
    }

    public RefreshResponseDto refresh(RefreshDto dto) {
        if (dto == null || dto.getRefreshToken() == null
                || !jwtServices.isValidRefreshToken(dto.getRefreshToken())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token"
            );
        }

        String email = jwtServices.extractEmail(dto.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired refresh token"
                ));

        return new RefreshResponseDto(
                jwtServices.generateToken(user.getEmail(), user.getRole()),
                jwtServices.getAccessExpirationSeconds()
        );
    }

    public MessageResponseDto logout(LogoutDto dto) {
        return new MessageResponseDto("Logged out");
    }

    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        userRepository.deleteById(userId);
    }

    @Transactional
    public boolean delete(UUID userId) {
        if (!userRepository.existsById(userId)) {
            return false;
        }

        userRepository.deleteById(userId);
        return true;
    }

    private boolean isValidEmail(String email) {
        return email != null
                && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }
}
