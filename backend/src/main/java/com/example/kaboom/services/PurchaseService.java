package com.example.kaboom.services;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PurchaseService(PurchaseRepository purchaseRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public Purchase buyProduct(UUID userId, UUID productId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setProduct(product);
        purchase.setPricePaid(product.getPrice());
        return purchaseRepository.save(purchase);
    }

    public List<Purchase> getUserPurchases(UUID userId) {
        return purchaseRepository.findByUserId(userId);
    }
}