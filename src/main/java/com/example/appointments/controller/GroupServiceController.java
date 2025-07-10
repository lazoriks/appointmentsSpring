package com.example.appointments.controller;

import com.example.appointments.entity.GroupService;
import com.example.appointments.repository.GroupServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // Отримати унікальні групи (group_id + group_name)
    @GetMapping("/list")
    public List<Map<String, Object>> groupList() {
        return repo.findDistinctGroups();
    }

    // Отримати послуги по конкретній групі
    @GetMapping("/{groupId}/services")
    public List<GroupService> servicesByGroup(@PathVariable Integer groupId) {
        return repo.findByGroupId(groupId);
    }
}

