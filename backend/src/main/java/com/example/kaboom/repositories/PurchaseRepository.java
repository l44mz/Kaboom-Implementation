package com.example.kaboom.repositories;

import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    List<Purchase> findByUserId(UUID userId);

    List<Purchase> findAllByUser_Id(UUID userId);

    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);

    Page<Purchase> findByUser_Id(UUID userId, Pageable pageable);

    // The user's purchase of one product (empty if they don't own it).
    Optional<Purchase> findByUser_IdAndProduct_Id(UUID userId, UUID productId);

    // The user's items in one category, used so only one hat (or screen...) is equipped.
    List<Purchase> findAllByUser_IdAndProduct_Category(UUID userId, Product.Category category);
}
