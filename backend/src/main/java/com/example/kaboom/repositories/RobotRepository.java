
package com.example.kaboom.repositories;

import com.example.kaboom.entities.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RobotRepository extends JpaRepository<Robot, UUID> {

    List<Robot> findByUserId(UUID userId);

    // Count robots that currently have the specified connection state.
    long countByConnectionState(Robot.ConnectionState connectionState);
}
