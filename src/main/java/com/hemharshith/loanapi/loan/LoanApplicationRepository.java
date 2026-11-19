package com.hemharshith.loanapi.loan;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Database access layer. We write NO code inside: by extending JpaRepository,
 * Spring generates save(), findById(), findAll(), deleteById() and more at
 * startup.
 * The two type arguments are: the entity class, and the type of its id.
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
}