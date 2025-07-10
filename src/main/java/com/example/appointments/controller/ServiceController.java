package com.example.appointments.controller;

import com.example.appointments.dto.ServiceShortDTO;
import com.example.appointments.repository.ServiceRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "https://glamlimerick.com")
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository repo;

    public ServiceController(ServiceRepository repo) {
        this.repo = repo;
    }

    // Всі сервіси (не DTO)
    @GetMapping
    public List<com.example.appointments.entity.Service> all() {
        return repo.findAll();
    }

    // DTO для короткого списку
    @GetMapping("/list")
    public List<ServiceShortDTO> getServiceList() {
        return repo.findAll().stream()
                .map(s -> new ServiceShortDTO(
                        s.getId(),
                        s.getServiceName(),
                        s.getPrice(),
                        s.getPeriod()
                ))
                .toList();
    }

    // DTO по групі
    @GetMapping("/group/{groupId}")
    public List<ServiceShortDTO> getByGroup(@PathVariable Integer groupId) {
        return repo.findByGroupServiceId(groupId).stream()
                .map(s -> new ServiceShortDTO(
                        s.getId(),
                        s.getServiceName(),
                        s.getPrice(),
                        s.getPeriod()
                ))
                .toList();
    }
}
