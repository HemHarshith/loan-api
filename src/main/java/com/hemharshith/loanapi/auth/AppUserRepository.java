package com.hemharshith.loanapi.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data builds the SQL for these two methods from their NAMES:
 * findByUsername -> SELECT ... WHERE username = ?
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}