package com.example.appointments.controller;

import com.example.appointments.dto.ServiceShortDTO;
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

    // Всі сервіси (не DTO)
    @GetMapping
    public List<Service> all() {
        return repo.findAll();
    }

    // DTO для короткого списку
    @GetMapping("/list")
    public List<ServiceShortDTO> getServiceList() {
        return repo.findAll().stream()
                .map(ServiceController::toShortDto)
                .toList();
    }

    // DTO по групі
    @GetMapping("/group/{groupId}")
    public List<ServiceShortDTO> getByGroup(@PathVariable Integer groupId) {
        return repo.findByGroupServiceId(groupId).stream()
                .map(ServiceController::toShortDto)
                .toList();
    }

    private static ServiceShortDTO toShortDto(Service s) {
        return new ServiceShortDTO(
                s.getId(),
                s.getServiceName(),
                s.getPrice(),
                s.getPeriod(),
                s.getDescription()
        );
    }
}
