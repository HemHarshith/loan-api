package com.hemharshith.loanapi.loan;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic layer, now backed by PostgreSQL through the repository.
 * The controller is unchanged: it still calls the same three methods and
 * gets the same response type back.
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    // Spring injects the repository it generated (constructor injection).
    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    // @Transactional: everything in the method succeeds together or is rolled back.
    @Transactional
    public LoanApplicationResponse create(LoanApplicationRequest request) {
        LoanApplication entity = new LoanApplication(
                request.applicantName(),
                request.age(),
                request.loanAmount(),
                request.durationMonths(),
                request.purpose(),
                LoanStatus.SUBMITTED, // every new application starts here
                Instant.now());
        // save() runs the INSERT; the returned object has the new id filled in.
        LoanApplication saved = repository.save(entity);
        return toResponse(saved);
    }

    // readOnly = true tells the database we only read, which allows optimisations.
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // Converts the database object into the JSON-shaped response object.
    // Keeping them separate means a table change does not automatically
    // change what the API returns to clients.
    private LoanApplicationResponse toResponse(LoanApplication e) {
        return new LoanApplicationResponse(
                e.getId(),
                e.getApplicantName(),
                e.getAge(),
                e.getLoanAmount(),
                e.getDurationMonths(),
                e.getPurpose(),
                e.getStatus(),
                e.getCreatedAt());
    }
}