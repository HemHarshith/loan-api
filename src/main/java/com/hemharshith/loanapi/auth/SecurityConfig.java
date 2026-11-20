package com.hemharshith.loanapi.auth;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Security rules: who may call what, and how tokens are signed and checked.
 * Step 4b adds: reading the "role" claim from the token, and
 * method-level rules such as @PreAuthorize("hasRole('ADMIN')").
 */
@Configuration
// Switches on Spring Security's web support and creates the HttpSecurity
// builder our filter chain needs. Spring Boot does this automatically in
// the full app, but not inside the @WebMvcTest slice, so we say it explicitly.
@EnableWebSecurity
// Turns on @PreAuthorize / @PostAuthorize checks on our controller methods.
@EnableMethodSecurity
public class SecurityConfig {

    // BCrypt turns a password into a one-way hash (random salt built in).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // The signing key, built from the JWT_SECRET environment variable.
    @Bean
    public SecretKey jwtKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 needs at least 32 bytes. Fail at startup if the key is too weak.
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Signs new tokens (used at login).
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    // Verifies tokens on every incoming request (signature and expiry).
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // By default Spring turns the token's "scope" claim into permissions and
    // ignores our "role" claim. This converter changes that:
    // - read the claim named "role"
    // - add the prefix "ROLE_" so "ADMIN" becomes the authority "ROLE_ADMIN"
    // hasRole('ADMIN') then looks for exactly "ROLE_ADMIN".
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // CSRF protects browser cookie sessions. We use header tokens and
                // no sessions, so it is switched off.
                .csrf(csrf -> csrf.disable())
                // STATELESS: the server remembers nothing between requests.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: register, login, ping and the health check.
                        .requestMatchers("/api/auth/**", "/api/ping", "/actuator/health").permitAll()
                        // Everything else needs a valid token. Finer role rules
                        // sit on the controller methods (@PreAuthorize).
                        .anyRequest().authenticated())
                // Read and verify "Authorization: Bearer <token>", then build the
                // user's permissions from the "role" claim using our converter.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}