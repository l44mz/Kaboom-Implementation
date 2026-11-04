package com.example.kaboom.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One purchase, as sent to the app.
 * (The Purchase entity itself is never returned, because it holds the whole User.)
 */
public record PurchaseResponse(
        UUID id,
        UUID productId,
        String productName,
        String category,
        int pricePaid,
        LocalDateTime purchasedAt,
        boolean equipped,
        int coinsLeft) {}
