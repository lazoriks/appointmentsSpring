package com.example.appointments.controller;

import com.example.appointments.dto.ServiceShortDTO;
import com.example.appointments.entity.Service;
import com.example.appointments.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
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

    @GetMapping("/list")
    public List<ServiceShortDTO> getServiceList() {
        return repo.findAll()
                .stream()
                .map(s -> new ServiceShortDTO(s.getId(), s.getServiceName()))
                .toList();
    }
}