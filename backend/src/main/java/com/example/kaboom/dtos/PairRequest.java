package com.example.kaboom.dtos;

import jakarta.validation.constraints.NotBlank;

public record PairRequest(@NotBlank String pairingCode) {}
