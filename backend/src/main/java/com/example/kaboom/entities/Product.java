package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private int price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_available")
    private boolean isAvailable = true;

    public enum Category {
        hat, screen, effect
    }

    public Product(String name, Category category, int price, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }
}