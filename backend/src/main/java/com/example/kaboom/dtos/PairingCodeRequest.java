package com.example.kaboom.dtos;

import jakarta.validation.constraints.NotBlank;

public record PairingCodeRequest(@NotBlank String serialNumber) {}
