package com.hemharshith.loanapi.loan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * Business logic layer. The controller talks to this class, never to storage.
 * TEMPORARY: data is kept in a Map in memory, so it disappears on restart.
 * In the next step this is replaced by PostgreSQL via Spring Data JPA.
 */
// @Service tells Spring to create one instance of this class and
// hand it to any class that asks for it (this is "dependency injection").
@Service
public class LoanApplicationService {

    private final Map<Long, LoanApplicationResponse> store = new ConcurrentHashMap<>();
    // Generates 1, 2, 3... safely even if two requests arrive together.
    private final AtomicLong idCounter = new AtomicLong(0);

    public LoanApplicationResponse create(LoanApplicationRequest request) {
        Long id = idCounter.incrementAndGet();
        LoanApplicationResponse saved = new LoanApplicationResponse(
                id,
                request.applicantName(),
                request.age(),
                request.loanAmount(),
                request.durationMonths(),
                request.purpose(),
                LoanStatus.SUBMITTED, // every new application starts here
                Instant.now());
        store.put(id, saved);
        return saved;
    }

    public LoanApplicationResponse getById(Long id) {
        LoanApplicationResponse found = store.get(id);
        if (found == null) {
            throw new LoanNotFoundException(id);
        }
        return found;
    }

    public List<LoanApplicationResponse> getAll() {
        return new ArrayList<>(store.values());
    }
}