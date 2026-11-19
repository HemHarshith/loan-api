package com.hemharshith.loanapi.auth;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Register and login logic. */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AppUser register(String username, String password) {
        if (users.existsByUsername(username)) {
            // 409 Conflict: that username is already taken.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        // Public sign-up always gets OFFICER. ADMIN is created another way later.
        AppUser user = new AppUser(username, passwordEncoder.encode(password),
                "OFFICER", Instant.now());
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public String login(String username, String password) {
        // One message for "no such user" and "wrong password", so an attacker
        // cannot use the response to find out which usernames exist.
        AppUser user = users.findByUsername(username)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return jwtService.createToken(user);
    }
}