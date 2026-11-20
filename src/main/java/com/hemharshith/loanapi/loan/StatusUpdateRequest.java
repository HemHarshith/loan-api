package com.hemharshith.loanapi.loan;

import jakarta.validation.constraints.NotNull;

/**
 * JSON for changing a loan's status, e.g. {"status":"APPROVED"}.
 * 
 * @NotNull rejects a missing status. A misspelt value (e.g. "APROVED")
 *          cannot be converted to the enum, and Spring returns 400 for that.
 */
public record StatusUpdateRequest(
        @NotNull(message = "status is required") LoanStatus status) {
}