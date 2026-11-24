package com.hemharshith.loanapi.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * End-to-end test of the Kafka flow with a REAL PostgreSQL (started in Docker)
 * and a
 * REAL Kafka broker (embedded, running inside the test JVM):
 *
 * create loan -> event on "loan.submitted" -> scoring consumer -> "loan.scored"
 * -> status consumer -> loan status in the database
 *
 * Docker Desktop must be running for the PostgreSQL container.
 * 
 * @ServiceConnection points the datasource at that container automatically.
 * @EmbeddedKafka starts a broker and, because of bootstrapServersProperty, sets
 *                spring.kafka.bootstrap-servers to it, so the app talks to this
 *                broker and not to
 *                the Kafka container you run by hand.
 *
 *                The test does NOT use @Transactional: the Kafka event is sent
 *                only AFTER the
 *                transaction commits, so the service call must really commit.
 */
@SpringBootTest(properties = {
                // The app normally reads these from environment variables; the test sets its
                // own.
                "app.jwt.secret=integration-test-secret-key-with-more-than-32-bytes",
                "spring.datasource.password=replaced-by-the-container"
})
@Testcontainers
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class LoanKafkaFlowTest {

        // The PostgreSQL JDBC driver sends the JVM's timezone name to the database.
        // Java on this laptop reports the old name "Asia/Calcutta", which the
        // postgres:17
        // container rejects ("invalid value for parameter TimeZone"). The modern name
        // "Asia/Kolkata" is accepted everywhere. This block runs once, before anything
        // connects, and only affects the JVM of this test run.
        static {
                TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

        @Autowired
        private LoanApplicationService service;

        @Autowired
        private LoanApplicationRepository repository;

        @Test
        void highRiskLoanMovesToUnderReviewAndLowRiskStaysSubmitted() {
                // Unique names so we can find "our" loans in the database.
                String highName = "HIGH-" + UUID.randomUUID();
                String lowName = "LOW-" + UUID.randomUUID();

                // Placeholder scoring: monthly repayment / 50,000, capped at 1.0.
                // 1,500,000 / 12 months -> score 1.0 -> HIGH
                // 120,000 / 24 months -> score 0.10 -> LOW
                service.create(new LoanApplicationRequest(
                                lowName, 30, new BigDecimal("120000"), 24, "Education"));
                service.create(new LoanApplicationRequest(
                                highName, 40, new BigDecimal("1500000"), 12, "Business"));

                // The flow is asynchronous, so keep checking (up to 40 s) until the HIGH loan
                // has been scored and moved to UNDER_REVIEW by the second consumer.
                await().atMost(Duration.ofSeconds(40)).pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> assertThat(statusOf(highName))
                                                .isEqualTo(LoanStatus.UNDER_REVIEW));

                // A LOW loan never changes status.
                assertThat(statusOf(lowName)).isEqualTo(LoanStatus.SUBMITTED);
        }

        // Reads the current status straight from the database.
        private LoanStatus statusOf(String applicantName) {
                return repository.findAll().stream()
                                .filter(l -> l.getApplicantName().equals(applicantName))
                                .findFirst()
                                .orElseThrow()
                                .getStatus();
        }
}