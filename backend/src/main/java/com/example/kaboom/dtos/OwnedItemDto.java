package com.example.kaboom.dtos;

import java.util.UUID;

/** A shop item the logged-in user owns, and whether Sparky is wearing it. */
public record OwnedItemDto(
        UUID productId,
        String name,
        String category,
        int pricePaid,
        boolean equipped) {}
