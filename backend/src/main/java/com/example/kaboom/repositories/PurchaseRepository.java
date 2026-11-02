
package com.example.kaboom.repositories;

import com.example.kaboom.entities.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    List<Purchase> findByUserId(UUID userId);

    List<Purchase> findAllByUser_Id(UUID userId);

    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);

    Page<Purchase> findByUser_Id(UUID userId, Pageable pageable);
}
