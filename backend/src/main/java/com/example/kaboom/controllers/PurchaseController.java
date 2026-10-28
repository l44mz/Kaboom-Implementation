
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreatePurchaseRequest;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.services.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/purchases")
@Tag(name = "Purchases", description = "Shop purchases and purchase history")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @Operation(summary = "Purchase a product")
    public ResponseEntity<Purchase> buyProduct(
            @Valid @RequestBody CreatePurchaseRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Purchase purchase = purchaseService.buyProduct(
                email, request.getProductId());

        return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
    }

    @GetMapping
    @Operation(summary = "Get authenticated user's purchase history")
    public ResponseEntity<Page<Purchase>> getUserPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        Page<Purchase> purchases = purchaseService.getUserPurchases(
                email, PageRequest.of(page, size));

        return ResponseEntity.ok(purchases);
    }
}
