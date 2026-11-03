package com.example.kaboom.repositories;

import com.example.kaboom.entities.StateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StateLogRepository extends JpaRepository<StateLog, UUID> {

    Optional<StateLog> findFirstByFocusSession_IdAndEndedAtIsNullOrderByStartedAtDesc(
            UUID sessionId
    );

    List<StateLog> findByFocusSession_IdOrderByStartedAtAsc(UUID sessionId);

    long countByFocusSession_IdAndState(
            UUID sessionId,
            StateLog.State state
    );
}