package com.hemharshith.loanapi.loan;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The JSON a client sends when applying for a loan.
 * A "record" is a short way to write a class that only holds data.
 * The annotations are validation rules: Spring checks them automatically
 * and rejects bad input before our own code runs.
 */
public record LoanApplicationRequest(

        @NotBlank(message = "applicantName is required") @Size(max = 100, message = "applicantName must be at most 100 characters") String applicantName,

        @NotNull(message = "age is required") @Min(value = 18, message = "applicant must be at least 18") @Max(value = 100, message = "age must be at most 100") Integer age,

        // BigDecimal is used for money because double can give rounding errors.
        @NotNull(message = "loanAmount is required") @DecimalMin(value = "1000.00", message = "loanAmount must be at least 1000") BigDecimal loanAmount,

        @NotNull(message = "durationMonths is required") @Min(value = 6, message = "durationMonths must be at least 6") @Max(value = 120, message = "durationMonths must be at most 120") Integer durationMonths,

        @NotBlank(message = "purpose is required") String purpose) {
}