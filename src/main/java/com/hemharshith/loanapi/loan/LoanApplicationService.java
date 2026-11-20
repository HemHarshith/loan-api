package com.hemharshith.loanapi.loan;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic layer, backed by PostgreSQL through the repository.
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

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
        LoanApplication saved = repository.save(entity);
        return toResponse(saved);
    }

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

    // Load the row, change one field, return the result.
    // We never call save(): inside a @Transactional method Hibernate notices
    // the entity changed ("dirty checking") and runs the UPDATE when the
    // transaction commits.
    @Transactional
    public LoanApplicationResponse updateStatus(Long id, LoanStatus newStatus) {
        LoanApplication entity = repository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException(id));
        entity.changeStatus(newStatus);
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        // Check first so a missing id gives our clean 404, not a database error.
        if (!repository.existsById(id)) {
            throw new LoanNotFoundException(id);
        }
        repository.deleteById(id);
    }

    // Converts the database object into the JSON-shaped response object.
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