package com.example.appointments.controller;

import com.example.appointments.entity.GroupService;
import com.example.appointments.repository.GroupServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupServiceController {
    private final GroupServiceRepository repo;

    public GroupServiceController(GroupServiceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<GroupService> all() {
        return repo.findAll();
    }
}