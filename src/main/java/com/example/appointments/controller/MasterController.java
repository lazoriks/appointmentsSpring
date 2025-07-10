package com.example.appointments.controller;

import com.example.appointments.dto.MasterShortDTO;
import com.example.appointments.entity.Master;
import com.example.appointments.repository.MasterRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/masters")
public class MasterController {

    private final MasterRepository repo;

    public MasterController(MasterRepository repo) {
        this.repo = repo;
    }

    // Отримати всіх майстрів (повна ентіті)
    @GetMapping
    public List<Master> all() {
        return repo.findAll();
    }

    // Отримати всіх майстрів по groupId (повна ентіті)
    @GetMapping("/group/{groupId}")
    public List<Master> getByGroup(@PathVariable Integer groupId) {
        return repo.findByGroupServiceId(groupId);
    }

    // Отримати лише DTO: id + fullName
    @GetMapping("/group/{groupId}/short")
    public List<MasterShortDTO> getShortByGroup(@PathVariable Integer groupId) {
        return repo.findByGroupServiceId(groupId).stream()
                .map(m -> new MasterShortDTO(
                        m.getId(),
                        m.getFirstName() + " " + m.getSurname()
                ))
                .toList();
    }
}
