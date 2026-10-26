package com.example.kaboom.repositories;

import com.example.kaboom.entities.FocusSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query("SELECT f FROM FocusSession f WHERE f.user.id = :userId AND f.status IN (com.example.kaboom.entities.FocusSession.SessionStatus.active, com.example.kaboom.entities.FocusSession.SessionStatus.paused)")
    Optional<FocusSession> findActiveOrPausedSessionByUserId(@Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FocusSession s where s.id = :id")
    Optional<FocusSession> findByIdForUpdate(@Param("id") UUID id);

    Optional<FocusSession> findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
            UUID robotId, Collection<FocusSession.SessionStatus> statuses);
}