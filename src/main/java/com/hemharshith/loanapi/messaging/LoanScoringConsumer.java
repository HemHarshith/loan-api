package com.hemharshith.loanapi.messaging;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.hemharshith.loanapi.loan.LoanSubmittedEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads "loan.submitted" messages, scores each loan, and publishes the result
 * to
 * "loan.scored".
 *
 * groupId = the consumer group. Consumers in the same group share the topic's
 * partitions between them, so adding a second copy of this app splits the work
 * and no loan is scored twice.
 *
 * IMPORTANT: the scoring rule below is a PLACEHOLDER (monthly repayment size),
 * not a trained model. Phase D will replace it with the Python loan-risk model.
 */
@Component
public class LoanScoringConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanScoringConsumer.class);

    public static final String SCORED_TOPIC = "loan.scored";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final KafkaTemplate<String, String> kafkaTemplate;

    public LoanScoringConsumer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Spring calls this method once for every message that arrives on the topic.
    @KafkaListener(topics = LoanEventPublisher.TOPIC, groupId = "loan-scoring")
    public void onLoanSubmitted(String json) {
        // Turn the JSON text back into the event object.
        LoanSubmittedEvent event = MAPPER.readValue(json, LoanSubmittedEvent.class);
        log.info("Scoring loan {} (amount {}, {} months)",
                event.loanId(), event.loanAmount(), event.durationMonths());

        double score = placeholderScore(event);
        String band = band(score);

        LoanScoredEvent scored = new LoanScoredEvent(
                event.loanId(), score, band, Instant.now().toString());

        // Same key (loan id) as before, so all events for one loan stay in order.
        kafkaTemplate.send(SCORED_TOPIC, String.valueOf(event.loanId()),
                MAPPER.writeValueAsString(scored));
        log.info("Published score for loan {}: {} ({})", event.loanId(), score, band);
    }

    // Placeholder: a bigger monthly repayment = a higher score, capped at 1.0.
    private double placeholderScore(LoanSubmittedEvent event) {
        BigDecimal monthly = event.loanAmount()
                .divide(BigDecimal.valueOf(event.durationMonths()), 2, RoundingMode.HALF_UP);
        double ratio = monthly.doubleValue() / 50_000.0;
        return Math.round(Math.min(1.0, ratio) * 100.0) / 100.0;
    }

    private String band(double score) {
        if (score >= 0.5) {
            return "HIGH";
        }
        if (score >= 0.15) {
            return "MEDIUM";
        }
        return "LOW";
    }
}