package com.hemharshith.loanapi.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka settings that go beyond application.properties:
 * 1) the topics (created at startup if missing);
 * 2) what happens when a consumer fails on a message.
 */
@Configuration
public class KafkaConfig {

    /**
     * Error handling for ALL listeners:
     * retry a failing message 3 more times, 1 second apart, then send it to a
     * "dead-letter topic" so the listener can move on to the next message.
     * Without this, one bad message would be retried forever and block the
     * partition.
     *
     * The destination is set explicitly to "<original topic>.DLT" (same partition
     * number). Spring's built-in default name differs between versions, and a
     * mismatch sends the message to a topic that does not exist.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    // Topics. Spring creates them at startup if they do not exist.
    // The dead-letter topics need the SAME number of partitions (3) as the
    // originals,
    // because a failed message is re-sent to the same partition number.
    @Bean
    NewTopic loanSubmittedTopic() {
        return TopicBuilder.name("loan.submitted").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic loanScoredTopic() {
        return TopicBuilder.name("loan.scored").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic loanSubmittedDeadLetterTopic() {
        return TopicBuilder.name("loan.submitted.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic loanScoredDeadLetterTopic() {
        return TopicBuilder.name("loan.scored.DLT").partitions(3).replicas(1).build();
    }
}