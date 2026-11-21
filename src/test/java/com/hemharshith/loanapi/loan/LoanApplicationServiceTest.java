package com.hemharshith.loanapi.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for LoanApplicationService.
 * A UNIT test checks one class alone. Everything it depends on (here the
 * repository) is replaced by a "mock": a fake object we control.
 * No Spring, no database: the test is fast and only fails if OUR logic is
 * wrong.
 * Each test follows the pattern Arrange (set up) / Act (call) / Assert (check).
 */
// Lets Mockito create the @Mock objects before each test.
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    // A fake repository. By default every method does nothing or returns empty.
    @Mock
    private LoanApplicationRepository repository;

    private LoanApplicationService service;

    // Runs before EVERY test, so each test starts with a fresh service.
    @BeforeEach
    void setUp() {
        // The service now also needs an event publisher. A Mockito mock is enough here:
        // these tests check the service logic, not Kafka.
        service = new LoanApplicationService(repository,
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    // Helper: builds an entity like one loaded from the database.
    // The entity has no setId (the database creates ids), so the test sets the
    // private field directly using Spring's ReflectionTestUtils.
    private LoanApplication sampleEntity(Long id, LoanStatus status) {
        LoanApplication entity = new LoanApplication(
                "Ravi", 30, new BigDecimal("50000.00"), 24, "car",
                status, Instant.now());
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    void create_startsAtSubmittedAndReturnsTheSavedValues() {
        // Arrange: save() should just return whatever it was given.
        LoanApplicationRequest request = new LoanApplicationRequest(
                "Ravi", 30, new BigDecimal("50000.00"), 24, "car");
        when(repository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoanApplicationResponse response = service.create(request);

        // Assert: capture the entity that was handed to the repository.
        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(response.status()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(response.applicantName()).isEqualTo("Ravi");
        assertThat(response.loanAmount()).isEqualByComparingTo("50000.00");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void getById_returnsTheLoanWhenItExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity(1L, LoanStatus.SUBMITTED)));

        LoanApplicationResponse response = service.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.purpose()).isEqualTo("car");
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(LoanNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_changesTheStatusWithoutCallingSave() {
        LoanApplication entity = sampleEntity(1L, LoanStatus.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        LoanApplicationResponse response = service.updateStatus(1L, LoanStatus.APPROVED);

        assertThat(response.status()).isEqualTo(LoanStatus.APPROVED);
        assertThat(entity.getStatus()).isEqualTo(LoanStatus.APPROVED);
        // We rely on Hibernate "dirty checking" to write the UPDATE,
        // so the service must NOT call save(). This line proves it.
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_throwsNotFoundWhenMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(7L, LoanStatus.REJECTED))
                .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void delete_removesTheLoanWhenItExists() {
        when(repository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(repository).deleteById(5L);
    }

    @Test
    void delete_throwsNotFoundAndDeletesNothingWhenMissing() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(LoanNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}