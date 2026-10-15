package com.example.kaboom.repositories;

import com.example.kaboom.entities.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query("SELECT f FROM FocusSession f WHERE f.user.id = :userId AND f.status IN (com.example.kaboom.entities.FocusSession.SessionStatus.active, com.example.kaboom.entities.FocusSession.SessionStatus.paused)")
    Optional<FocusSession> findActiveOrPausedSessionByUserId(@Param("userId") UUID userId);
}