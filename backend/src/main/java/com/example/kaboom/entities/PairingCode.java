package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor

@Entity
@Table(name = "pairing_codes")
public class PairingCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    // 6 digits. Not unique: an expired code's value can be reused later.
    @Column(name = "code", nullable = false, length = 6)
    private String code;

    // the secret is never stored
    @Column(name = "secret_hash", nullable = false, unique = true, length = 64)
    private String secretHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_user_id")
    private User claimedByUser;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id")
    private Robot robot;


    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

}