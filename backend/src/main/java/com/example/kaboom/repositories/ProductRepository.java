
package com.example.kaboom.repositories;

import com.example.kaboom.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Retrieve only products that are currently available.
    List<Product> findByIsAvailableTrue();

// Retrieve all products, including hidden products, for admin management.
    List<Product> findAllByOrderByIdAsc();

}
