package com.example.kaboom.repositories;

import com.example.kaboom.entities.FocusSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query("SELECT f FROM FocusSession f WHERE f.user.id = :userId AND f.status IN (com.example.kaboom.entities.FocusSession.SessionStatus.active, com.example.kaboom.entities.FocusSession.SessionStatus.paused)")
    Optional<FocusSession> findActiveOrPausedSessionByUserId(@Param("userId") UUID userId);

    // Lock the session row to prevent concurrent requests from awarding coins twice.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FocusSession f WHERE f.id = :sessionId")
    Optional<FocusSession> findByIdForUpdate(@Param("sessionId") UUID sessionId);

    // Count focus sessions that started within the specified day.
    @Query("SELECT COUNT(f) FROM FocusSession f WHERE f.startTime >= :start AND f.startTime < :end")
    long countSessionsStartedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}