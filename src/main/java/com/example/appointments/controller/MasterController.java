package com.example.appointments.controller;

import com.example.appointments.entity.Master;
import com.example.appointments.repository.MasterRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/masters")
public class MasterController {
    private final MasterRepository repo;

    public MasterController(MasterRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Master> all() {
        return repo.findAll();
    }
}