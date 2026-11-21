package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreatePurchaseRequest;
import com.example.kaboom.dtos.PurchaseResponse;
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

@RestController
@RequestMapping("/purchases")
@Tag(name = "Purchases", description = "Shop purchases and purchase history")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Operation(
            summary = "Buy a product",
            description = "Buys one product for the logged-in user. Returns the purchase and the coins left. "
                    + "400 = not enough coins, 409 = already owned or not in the shop."
    )
    @PostMapping
    public ResponseEntity<PurchaseResponse> buyProduct(
            @Valid @RequestBody CreatePurchaseRequest request) {

        PurchaseResponse purchase = purchaseService.buyProduct(
                currentEmail(), request.getProductId());

        return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
    }

    @Operation(summary = "My purchases", description = "The logged-in user's purchases, one page at a time.")
    @GetMapping
    public ResponseEntity<Page<PurchaseResponse>> getUserPurchases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                purchaseService.getUserPurchases(currentEmail(), PageRequest.of(page, size)));
    }

    private String currentEmail() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
