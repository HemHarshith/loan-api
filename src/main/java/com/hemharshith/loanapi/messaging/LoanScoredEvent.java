package com.hemharshith.loanapi.messaging;

/**
 * The "a loan was scored" event, published to the topic "loan.scored".
 * score = a number between 0 and 1 (higher = riskier).
 * band = LOW, MEDIUM or HIGH, using the same cut-offs (0.15 / 0.5) as
 * loan-risk.
 */
public record LoanScoredEvent(
        Long loanId,
        double score,
        String band,
        String scoredAt) {
}