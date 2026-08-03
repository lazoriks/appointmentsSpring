package com.example.appointments.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

public class AdminApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Admin-Key";

    private final String adminApiKey;
    private final Set<String> protectedMethods; // null = protect every method

    public AdminApiKeyFilter(String adminApiKey) {
        this(adminApiKey, null);
    }

    public AdminApiKeyFilter(String adminApiKey, Set<String> protectedMethods) {
        this.adminApiKey = adminApiKey;
        this.protectedMethods = protectedMethods;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (protectedMethods != null && !protectedMethods.contains(request.getMethod().toUpperCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER_NAME);

        if (!isAuthorized(providedKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(String providedKey) {
        if (adminApiKey == null || adminApiKey.isBlank() || providedKey == null) {
            return false;
        }
        byte[] expected = adminApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
