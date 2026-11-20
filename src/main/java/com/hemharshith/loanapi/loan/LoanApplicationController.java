package com.hemharshith.loanapi.loan;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** HTTP layer: receives requests, calls the service, returns JSON. */
@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    // POST /api/loan-applications (any logged-in user)
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> create(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/loan-applications/" + created.id()))
                .body(created);
    }

    // GET /api/loan-applications/5 (any logged-in user)
    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // GET /api/loan-applications (any logged-in user)
    @GetMapping
    public List<LoanApplicationResponse> getAll() {
        return service.getAll();
    }

    // PATCH /api/loan-applications/5/status (ADMIN only)
    // PATCH = change part of a resource (here only the status).
    // @PreAuthorize runs BEFORE the method body. If the caller lacks the
    // ADMIN role, Spring throws AccessDeniedException and the client gets 403.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public LoanApplicationResponse updateStatus(@PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request.status());
    }

    // DELETE /api/loan-applications/5 (ADMIN only)
    // 204 No Content = success, nothing to send back.
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}