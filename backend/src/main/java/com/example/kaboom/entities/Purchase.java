package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchases")
@NoArgsConstructor
@Getter
@Setter
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price_paid", nullable = false)
    private int pricePaid;

    @Column(name = "purchased_at", nullable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "is_equipped")
    private boolean isEquipped = false;

    public Purchase(User user, Product product, int pricePaid) {
        this.user = user;
        this.product = product;
        this.pricePaid = pricePaid;
        this.purchasedAt = LocalDateTime.now();
    }
}