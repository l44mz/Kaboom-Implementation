
package com.example.kaboom.services;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            UserRepository userRepository,
            ProductRepository productRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Purchase buyProduct(String email, UUID productId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isAvailable()) {
            throw new IllegalStateException("Product is not available");
        }

        if (purchaseRepository.existsByUser_IdAndProduct_Id(
                user.getId(), productId)) {
            throw new IllegalStateException("You already own this product");
        }

        if (user.getCoins() < product.getPrice()) {
            throw new IllegalStateException("Insufficient coins");
        }

        // Deduct the product price from the user's coin balance.
        user.setCoins(user.getCoins() - product.getPrice());
        userRepository.save(user);

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setProduct(product);

        // Set the quantity to one item per purchase request.
        purchase.setQuantity(1);

        // Save the price paid for a single item.
        purchase.setPricePaid(product.getPrice());

        // Calculate and store the total purchase price.
        purchase.setTotalPrice(
                product.getPrice() * purchase.getQuantity()
        );

        purchase.setPurchasedAt(LocalDateTime.now());

        return purchaseRepository.save(purchase);
    }

    public Page<Purchase> getUserPurchases(
            String email, Pageable pageable) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return purchaseRepository.findByUser_Id(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> getUserItems(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return purchaseRepository.findAllByUser_Id(user.getId())
                .stream()
                .map(Purchase::getProduct)
                .toList();
    }
}
