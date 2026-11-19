package com.hemharshith.loanapi.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public endpoints: POST /api/auth/register and POST /api/auth/login. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Request/response shapes kept next to the only class that uses them.
    // Max 72 characters on password: BCrypt ignores anything beyond 72 bytes.
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterResponse(Long id, String username, String role) {
    }

    public record TokenResponse(String accessToken, String tokenType) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = authService.register(request.username(), request.password());
        // Never send the password hash back to the client.
        return new RegisterResponse(user.getId(), user.getUsername(), user.getRole());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return new TokenResponse(token, "Bearer");
    }
}