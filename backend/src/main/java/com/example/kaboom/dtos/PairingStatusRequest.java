package com.example.kaboom.dtos;

import jakarta.validation.constraints.NotBlank;

public record PairingStatusRequest(@NotBlank String pairingSecret) {}
