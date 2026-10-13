package com.example.kaboom.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    private int coins;
    private String role;

}
