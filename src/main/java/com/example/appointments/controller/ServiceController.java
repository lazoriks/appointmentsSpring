package com.example.appointments.controller;

import com.example.appointments.entity.Service;
import com.example.appointments.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final ServiceRepository repo;

    public ServiceController(ServiceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Service> all() {
        return repo.findAll();
    }
}