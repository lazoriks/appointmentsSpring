package com.example.appointments.controller;

import com.example.appointments.dto.GroupServiceShortDTO;
import com.example.appointments.entity.GroupService;
import com.example.appointments.repository.GroupServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    // (group_id + group_name)
    @GetMapping("/list")
    public List<GroupServiceShortDTO> getGroupsShort() {
        return repo.findAll().stream()
                .map(g -> new GroupServiceShortDTO(g.getId(), g.getGroupName()))
                .toList();
    }

    @GetMapping("/{id}")
    public GroupService getGroup(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

}

