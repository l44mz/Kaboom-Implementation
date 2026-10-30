
package com.example.kaboom.repositories;

import com.example.kaboom.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    // Count regular users without including admin accounts.
    long countByRole(String role);
}
