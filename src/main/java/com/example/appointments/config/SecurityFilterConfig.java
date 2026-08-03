package com.example.appointments.config;

import com.example.appointments.security.AdminApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class SecurityFilterConfig {

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Bean
    public FilterRegistrationBean<AdminApiKeyFilter> adminApiKeyFilterRegistration() {
        FilterRegistrationBean<AdminApiKeyFilter> registration =
                new FilterRegistrationBean<>(new AdminApiKeyFilter(adminApiKey));
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(1);
        return registration;
    }

    // Protects only GET /api/clients (the full client list). "/api/clients" is an
    // exact servlet url-pattern, so it does NOT match /api/clients/search or the
    // public POST /api/clients (client self-registration from the booking form).
    @Bean
    public FilterRegistrationBean<AdminApiKeyFilter> clientsListApiKeyFilterRegistration() {
        FilterRegistrationBean<AdminApiKeyFilter> registration =
                new FilterRegistrationBean<>(new AdminApiKeyFilter(adminApiKey, Set.of("GET")));
        registration.addUrlPatterns("/api/clients");
        registration.setOrder(1);
        return registration;
    }
}
