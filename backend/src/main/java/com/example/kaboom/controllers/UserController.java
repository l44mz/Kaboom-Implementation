package com.example.kaboom.controllers;

import com.example.kaboom.entities.User;
import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.mappers.UserMapper;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.UserServices;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserServices userServices;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userServices.getUserById(id));
    }
}