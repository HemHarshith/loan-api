package com.hemharshith.loanapi.loan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hemharshith.loanapi.auth.SecurityConfig;

/**
 * Web-layer tests for LoanApplicationController.
 * 
 * @WebMvcTest starts ONLY the web part of Spring: controllers, the
 * @RestControllerAdvice (GlobalExceptionHandler) and the security filters.
 *                       MockMvc sends fake HTTP requests to it without starting
 *                       a real server.
 */
// Slice tests do not pick up @Configuration classes by themselves, so we
// import our real SecurityConfig. That way the 401 and 403 rules tested here
// are the same ones the running app uses.
@WebMvcTest(controllers = LoanApplicationController.class,
        // SecurityConfig reads this property. It is a fake value used only in
        // tests (it just has to be 32+ characters), not a real secret.
        properties = "app.jwt.secret=test-only-secret-key-at-least-32-chars-long!!")
@Import(SecurityConfig.class)
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the real service with a mock inside the Spring context.
    @MockitoBean
    private LoanApplicationService service;

    // --- helpers: pretend a token with the given role was sent ---
    // jwt() skips real signing and puts a logged-in user straight into the
    // security context. We set the authority ourselves; ROLE_ADMIN is what
    // hasRole('ADMIN') looks for.
    private JwtRequestPostProcessor officer() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OFFICER"));
    }

    private JwtRequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private LoanApplicationResponse sampleResponse(LoanStatus status) {
        return new LoanApplicationResponse(1L, "Ravi", 30, new BigDecimal("50000.00"),
                24, "car", status, Instant.now());
    }

    // ---------- reading ----------

    @Test
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/loan-applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_asOfficer_returns200WithAnArray() throws Exception {
        when(service.getAll()).thenReturn(List.of(sampleResponse(LoanStatus.SUBMITTED)));

        mockMvc.perform(get("/api/loan-applications").with(officer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicantName").value("Ravi"));
    }

    @Test
    void getById_missing_returns404WithMessage() throws Exception {
        when(service.getById(99L)).thenThrow(new LoanNotFoundException(99L));

        mockMvc.perform(get("/api/loan-applications/99").with(officer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Loan application 99 not found"));
    }

    // ---------- creating ----------

    @Test
    void create_validRequest_returns201WithLocationHeader() throws Exception {
        when(service.create(any(LoanApplicationRequest.class)))
                .thenReturn(sampleResponse(LoanStatus.SUBMITTED));

        mockMvc.perform(post("/api/loan-applications")
                .with(officer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"applicantName":"Ravi","age":30,"loanAmount":50000,
                         "durationMonths":24,"purpose":"car"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/loan-applications/1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void create_invalidRequest_returns400ListingTheBadFields() throws Exception {
        // age 15 is under 18 and applicantName is missing.
        mockMvc.perform(post("/api/loan-applications")
                .with(officer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"age":15,"loanAmount":50000,"durationMonths":24,"purpose":"car"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.applicantName").exists())
                .andExpect(jsonPath("$.fields.age").exists());

        // Validation stops the request BEFORE the service is reached.
        verifyNoInteractions(service);
    }

    // ---------- changing status (ADMIN only) ----------

    @Test
    void updateStatus_asOfficer_returns403AndServiceIsNotCalled() throws Exception {
        mockMvc.perform(patch("/api/loan-applications/1/status")
                .with(officer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void updateStatus_asAdmin_returns200() throws Exception {
        when(service.updateStatus(1L, LoanStatus.APPROVED))
                .thenReturn(sampleResponse(LoanStatus.APPROVED));

        mockMvc.perform(patch("/api/loan-applications/1/status")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void updateStatus_misspeltStatus_returns400() throws Exception {
        // "APROVED" cannot be converted to the LoanStatus enum.
        mockMvc.perform(patch("/api/loan-applications/1/status")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APROVED\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- deleting (ADMIN only) ----------

    @Test
    void delete_asOfficer_returns403AndServiceIsNotCalled() throws Exception {
        mockMvc.perform(delete("/api/loan-applications/1").with(officer()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void delete_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/loan-applications/1").with(admin()))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void delete_missingLoan_asAdmin_returns404() throws Exception {
        // delete() returns void, so the "make it throw" syntax starts with doThrow.
        doThrow(new LoanNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/api/loan-applications/99").with(admin()))
                .andExpect(status().isNotFound());
    }
}