package com.example.kaboom.controllers;


import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.mappers.UserMapper;
import com.example.kaboom.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private UserRepository userRepository;
    private UserMapper userMapper;



}
