package com.example.kaboom.dtos;

import java.time.LocalTime;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String email;
    private String name;
    private int coins;
    private String role;
    private LocalTime createdAt;


}
