package com.example.appointments.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.api-key:}")
    private String adminApiKey;

    // Exchanges the salon-facing username/password for the internal X-Admin-Key,
    // so managers never see or type the raw key used by AdminApiKeyFilter.
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        if (!matches(adminUsername, request.username()) || !matches(adminPassword, request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return Map.of("apiKey", adminApiKey);
    }

    private boolean matches(String expected, String provided) {
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record LoginRequest(String username, String password) {}
}
