package com.hemharshith.loanapi.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hemharshith.loanapi.loan.LoanSubmittedEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * Sends loan events to Kafka.
 *
 * AFTER_COMMIT means this method runs only once the database transaction that
 * saved the loan has COMMITTED. If the save is rolled back, no event is sent,
 * so Kafka never announces a loan that does not exist in the database.
 */
@Component
public class LoanEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoanEventPublisher.class);

    // The topic (named channel) that the messages go to.
    public static final String TOPIC = "loan.submitted";

    // Converts the event object into a JSON text.
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // Spring Boot creates this KafkaTemplate for us from the spring.kafka.*
    // settings.
    private final KafkaTemplate<String, String> kafkaTemplate;

    public LoanEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanSubmitted(LoanSubmittedEvent event) {
        try {
            String json = MAPPER.writeValueAsString(event);

            // The KEY decides the partition: messages with the same key always go to
            // the same partition, so events for one loan stay in order.
            kafkaTemplate.send(TOPIC, String.valueOf(event.loanId()), json)
                    .whenComplete((result, error) -> {
                        // send() is asynchronous: this runs later, when Kafka answers.
                        if (error != null) {
                            log.error("Could not publish loan {} to {}", event.loanId(), TOPIC, error);
                        } else {
                            log.info("Published loan {} to {} (partition {}, offset {})",
                                    event.loanId(), TOPIC,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            // A Kafka problem must never make "create loan" return an error:
            // the loan is already saved. We log it instead.
            log.error("Failed to send loan {} event", event.loanId(), e);
        }
    }
}