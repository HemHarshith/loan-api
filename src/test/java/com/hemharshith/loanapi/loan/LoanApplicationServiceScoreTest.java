package com.hemharshith.loanapi.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Tests for applyScore(): the status changes only for HIGH scores on a
 * SUBMITTED
 * loan, a repeated message changes nothing, and a missing loan is skipped.
 * No Spring, no database, no Kafka: the repository is a Mockito mock.
 */
class LoanApplicationServiceScoreTest {

    private LoanApplicationRepository repository;
    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LoanApplicationRepository.class);
        service = new LoanApplicationService(repository, mock(ApplicationEventPublisher.class));
    }

    // Helper: a loan in the given status. The id is not needed because the mock
    // repository returns this object for findById(1L).
    private LoanApplication loan(LoanStatus status) {
        return new LoanApplication("ABC", 23, new BigDecimal("540000"), 24,
                "Education", status, Instant.now());
    }

    @Test
    void highScoreMovesSubmittedLoanToUnderReview() {
        LoanApplication loan = loan(LoanStatus.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        boolean changed = service.applyScore(1L, "HIGH");

        assertTrue(changed);
        assertEquals(LoanStatus.UNDER_REVIEW, loan.getStatus());
    }

    @Test
    void lowScoreLeavesStatusUnchanged() {
        LoanApplication loan = loan(LoanStatus.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        assertFalse(service.applyScore(1L, "LOW"));
        assertEquals(LoanStatus.SUBMITTED, loan.getStatus());
    }

    @Test
    void repeatedMessageDoesNotChangeAnythingTwice() {
        LoanApplication loan = loan(LoanStatus.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        assertTrue(service.applyScore(1L, "HIGH")); // first delivery changes it
        assertFalse(service.applyScore(1L, "HIGH")); // duplicate delivery: no change
        assertEquals(LoanStatus.UNDER_REVIEW, loan.getStatus());
    }

    @Test
    void loanAlreadyDecidedIsNotOverwritten() {
        LoanApplication loan = loan(LoanStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(loan));

        assertFalse(service.applyScore(1L, "HIGH"));
        assertEquals(LoanStatus.APPROVED, loan.getStatus());
    }

    @Test
    void missingLoanIsSkippedWithoutError() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(service.applyScore(99L, "HIGH"));
    }
}