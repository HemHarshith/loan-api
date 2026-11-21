package com.hemharshith.loanapi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hemharshith.loanapi.loan.LoanApplicationService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads "loan.scored" messages and applies the result to the loan in the
 * database.
 * Its own consumer group ("loan-status-updater") means it gets every message
 * independently of the scoring consumer.
 *
 * If this method throws (for example, the JSON is invalid), the error handler
 * in
 * KafkaConfig retries and finally moves the message to "loan.scored.DLT".
 */
@Component
public class LoanScoredConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanScoredConsumer.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final LoanApplicationService loanService;

    public LoanScoredConsumer(LoanApplicationService loanService) {
        this.loanService = loanService;
    }

    @KafkaListener(topics = LoanScoringConsumer.SCORED_TOPIC, groupId = "loan-status-updater")
    public void onLoanScored(String json) {
        LoanScoredEvent event = MAPPER.readValue(json, LoanScoredEvent.class);

        boolean changed = loanService.applyScore(event.loanId(), event.band());

        if (changed) {
            log.info("Loan {} moved to UNDER_REVIEW (band {})", event.loanId(), event.band());
        } else {
            // Not an error: band was not HIGH, the loan no longer exists, or it was
            // already processed (duplicate message).
            log.info("Loan {}: no status change (band {})", event.loanId(), event.band());
        }
    }
}