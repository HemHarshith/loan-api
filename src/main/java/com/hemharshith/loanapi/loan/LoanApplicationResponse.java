package com.hemharshith.loanapi.loan;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The JSON we send back. It has the request fields plus values the
 * server decides: id, status and creation time. Clients cannot set those.
 */
public record LoanApplicationResponse(
        Long id,
        String applicantName,
        Integer age,
        BigDecimal loanAmount,
        Integer durationMonths,
        String purpose,
        LoanStatus status,
        Instant createdAt) {
}