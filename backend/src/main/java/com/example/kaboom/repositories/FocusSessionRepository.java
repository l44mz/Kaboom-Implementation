package com.example.kaboom.repositories;

import com.example.kaboom.entities.FocusSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    @Query("SELECT f FROM FocusSession f WHERE f.user.id = :userId AND f.status IN (com.example.kaboom.entities.FocusSession.SessionStatus.active, com.example.kaboom.entities.FocusSession.SessionStatus.paused)")
    Optional<FocusSession> findActiveOrPausedSessionByUserId(@Param("userId") UUID userId);

    // Count focus sessions that started within the specified day.
    @Query("SELECT COUNT(f) FROM FocusSession f WHERE f.startTime >= :start AND f.startTime < :end")
    long countSessionsStartedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Loads a session and locks its row, so two robot reports can't update it at the same time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FocusSession f WHERE f.id = :id")
    Optional<FocusSession> findByIdForUpdate(@Param("id") UUID id);

    // The robot's newest session with one of the given statuses (used by the robot's polling).
    Optional<FocusSession> findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
            UUID robotId, Collection<FocusSession.SessionStatus> statuses);

    // The user's sessions with this status that started on or after "from", newest first (History).
    List<FocusSession> findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
            UUID userId, FocusSession.SessionStatus status, LocalDateTime from);

    // All the user's sessions with this status, newest first (History "All time").
    List<FocusSession> findByUser_IdAndStatusOrderByStartTimeDesc(
            UUID userId, FocusSession.SessionStatus status);
}
