package com.example.kaboom.dtos;

public record LoginResponseDto(String accessToken, String refreshToken,
                               long expiresInSeconds, UserDto user) {

}