package com.example.kaboom.repositories;

import com.example.kaboom.entities.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Provides database queries dedicated to the user's focus history.
 * Keeps History queries separate from focus session lifecycle operations.
 */
@Repository
public interface HistoryRepository extends JpaRepository<FocusSession, UUID> {

    /**
     * Retrieves all sessions belonging to a user,
     * ordered from newest to oldest.
     */
    List<FocusSession> findByUser_IdOrderByStartTimeDesc(UUID userId);

    /**
     * Retrieves a user's sessions within a date range.
     * The start time is inclusive and the end time is exclusive.
     */
    List<FocusSession> findByUser_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end
    );
}