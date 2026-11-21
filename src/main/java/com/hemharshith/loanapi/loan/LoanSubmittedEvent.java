package com.hemharshith.loanapi.loan;

import java.math.BigDecimal;

/**
 * The "a loan was submitted" event.
 * A record is a short way to write an immutable data holder: the fields below
 * become the constructor, the getters (loanId(), age(), ...) and
 * equals/toString.
 *
 * The same object is used twice:
 * 1) inside the app, as a Spring event raised by LoanApplicationService;
 * 2) converted to JSON and sent to the Kafka topic "loan.submitted".
 * We include only the fields a scoring service would need, not everything.
 */
public record LoanSubmittedEvent(
        Long loanId,
        String applicantName,
        Integer age,
        BigDecimal loanAmount,
        Integer durationMonths,
        String purpose,
        String createdAt) {
}