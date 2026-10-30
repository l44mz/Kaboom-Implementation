package com.example.kaboom.repositories;

import com.example.kaboom.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductId(UUID productId);

    void deleteByProductId(UUID productId);
}
