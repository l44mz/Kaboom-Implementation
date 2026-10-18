package com.example.kaboom.dtos;

public record LoginResponseDto(String accessToken,
                               long expiresInSeconds, UserDto user) {

}