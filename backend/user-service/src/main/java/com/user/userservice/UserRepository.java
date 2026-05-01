package com.user.userservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByDisplayName(String displayName); // ← était findByUsername
    boolean existsByEmail(String email);
    boolean existsByDisplayName(String displayName);      // ← était existsByUsername
}