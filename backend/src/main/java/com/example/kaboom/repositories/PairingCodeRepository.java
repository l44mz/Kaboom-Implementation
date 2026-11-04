package com.example.kaboom.repositories;

import com.example.kaboom.entities.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PairingCodeRepository extends JpaRepository<PairingCode, UUID> {

    Optional<PairingCode> findBySecretHash(String secretHash);

    Optional<PairingCode> findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
            String code, LocalDateTime now);

    boolean existsByCodeAndConsumedFalseAndExpiresAtAfter(String code, LocalDateTime now);

    void deleteBySerialNumberAndConsumedFalse(String serialNumber);
}