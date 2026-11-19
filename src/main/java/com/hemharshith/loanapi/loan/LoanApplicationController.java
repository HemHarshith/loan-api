package com.hemharshith.loanapi.loan;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** HTTP layer: receives requests, calls the service, returns JSON. */
@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    // Spring passes the service in here automatically (constructor injection).
    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    // POST /api/loan-applications
    // @Valid makes Spring run the validation rules from the request record.
    @PostMapping
    public ResponseEntity<LoanApplicationResponse> create(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse created = service.create(request);
        // 201 Created + a Location header pointing to the new resource.
        return ResponseEntity
                .created(URI.create("/api/loan-applications/" + created.id()))
                .body(created);
    }

    // GET /api/loan-applications/5
    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // GET /api/loan-applications
    @GetMapping
    public List<LoanApplicationResponse> getAll() {
        return service.getAll();
    }
}