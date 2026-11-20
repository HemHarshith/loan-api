package com.hemharshith.loanapi.loan;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Database model: one object of this class = one row in loan_application.
 * Spring Boot maps camelCase field names to snake_case column names
 * automatically (applicantName -> applicant_name).
 * Hibernate checks this class against the table at startup because
 * ddl-auto=validate, so a mismatch stops the app with a clear message.
 */
@Entity
@Table(name = "loan_application")
public class LoanApplication {

    // @Id = primary key. IDENTITY means the database (BIGSERIAL) creates the id.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String applicantName;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(nullable = false, length = 100)
    private String purpose;

    // STRING stores the enum NAME ("SUBMITTED") instead of its position (0).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    // JPA requires an empty constructor. "protected" stops our own code from using
    // it.
    protected LoanApplication() {
    }

    // The constructor our service uses. The id is not passed: the database creates
    // it.
    public LoanApplication(String applicantName, Integer age, BigDecimal loanAmount,
            Integer durationMonths, String purpose,
            LoanStatus status, Instant createdAt) {
        this.applicantName = applicantName;
        this.age = age;
        this.loanAmount = loanAmount;
        this.durationMonths = durationMonths;
        this.purpose = purpose;
        this.status = status;
        this.createdAt = createdAt;
    }

    // The one allowed change after creation. We expose a named method instead
    // of a general setStatus so the intent is clear and other fields stay fixed.
    public void changeStatus(LoanStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public Integer getAge() {
        return age;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public String getPurpose() {
        return purpose;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}