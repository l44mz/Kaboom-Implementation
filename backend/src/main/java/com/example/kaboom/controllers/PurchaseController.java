package com.example.kaboom.controllers;

import com.example.kaboom.entities.Purchase;
import com.example.kaboom.services.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;
    public PurchaseController(PurchaseService purchaseService) { this.purchaseService = purchaseService; }
    @PostMapping
    public ResponseEntity<Purchase> buyProduct(@RequestParam UUID userId, @RequestParam UUID productId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.buyProduct(userId, productId));
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Purchase>> getUserPurchases(@PathVariable UUID userId) {
        return ResponseEntity.ok(purchaseService.getUserPurchases(userId));
    }
}