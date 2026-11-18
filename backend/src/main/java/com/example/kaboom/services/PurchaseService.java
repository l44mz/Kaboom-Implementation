package com.example.kaboom.services;

import com.example.kaboom.dtos.OwnedItemDto;
import com.example.kaboom.dtos.PurchaseResponse;
import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.ProductRepository;
import com.example.kaboom.repositories.PurchaseRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Buying shop items with coins, and choosing which owned items Sparky wears.
 */
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

    /**
     * Buys one product for the logged-in user and takes the coins.
     * Errors: 404 product not found, 409 not available / already owned, 400 not enough coins.
     */
    @Transactional
    public PurchaseResponse buyProduct(String email, UUID productId) {

        // Locks the user's row, so two purchases at the same time can't spend the same coins.
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This item is not in the shop right now");
        }

        if (purchaseRepository.existsByUser_IdAndProduct_Id(user.getId(), productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You already own this item");
        }

        if (user.getCoins() < product.getPrice()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Not enough coins");
        }

        // Take the price from the user's coins.
        user.setCoins(user.getCoins() - product.getPrice());
        userRepository.save(user);

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setProduct(product);
        purchase.setQuantity(1);
        purchase.setPricePaid(product.getPrice());
        purchase.setTotalPrice(product.getPrice() * purchase.getQuantity());
        purchase.setPurchasedAt(LocalDateTime.now());

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved, user.getCoins());
    }

    /** The user's purchases, one page at a time. */
    @Transactional(readOnly = true)
    public Page<PurchaseResponse> getUserPurchases(String email, Pageable pageable) {
        User user = findUser(email);
        return purchaseRepository.findByUser_Id(user.getId(), pageable)
                .map(purchase -> toResponse(purchase, user.getCoins()));
    }

    /** Every item the user owns, and whether it is equipped. */
    @Transactional(readOnly = true)
    public List<OwnedItemDto> getUserItems(String email) {
        User user = findUser(email);
        return purchaseRepository.findAllByUser_Id(user.getId())
                .stream()
                .map(this::toOwnedItem)
                .toList();
    }

    /**
     * Puts an owned item on Sparky (equipped = true) or takes it off (false).
     * Sparky wears one item per category, so equipping a hat takes off the other hats.
     */
    @Transactional
    public OwnedItemDto setEquipped(String email, UUID productId, boolean equipped) {
        User user = findUser(email);

        Purchase purchase = purchaseRepository
                .findByUser_IdAndProduct_Id(user.getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You don't own this item"));

        if (equipped) {
            Product.Category category = purchase.getProduct().getCategory();
            for (Purchase other : purchaseRepository
                    .findAllByUser_IdAndProduct_Category(user.getId(), category)) {
                if (!other.getId().equals(purchase.getId()) && other.isEquipped()) {
                    other.setEquipped(false);
                    purchaseRepository.save(other);
                }
            }
        }

        purchase.setEquipped(equipped);
        return toOwnedItem(purchaseRepository.save(purchase));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private OwnedItemDto toOwnedItem(Purchase purchase) {
        Product product = purchase.getProduct();
        return new OwnedItemDto(
                product.getId(),
                product.getName(),
                product.getCategory().name(),
                purchase.getPricePaid(),
                purchase.isEquipped());
    }

    private PurchaseResponse toResponse(Purchase purchase, int coinsLeft) {
        Product product = purchase.getProduct();
        return new PurchaseResponse(
                purchase.getId(),
                product.getId(),
                product.getName(),
                product.getCategory().name(),
                purchase.getPricePaid(),
                purchase.getPurchasedAt(),
                purchase.isEquipped(),
                coinsLeft);
    }
}
