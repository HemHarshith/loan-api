package com.hemharshith.loanapi.loan;

/** Thrown when a loan application id does not exist. Becomes a 404 later. */
public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long id) {
        super("Loan application " + id + " not found");
    }
}