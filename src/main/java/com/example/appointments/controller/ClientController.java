package com.example.appointments.controller;

import com.example.appointments.entity.Client;
import com.example.appointments.repository.ClientRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientRepository repo;

    public ClientController(ClientRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Client> all() {
        return repo.findAll();
    }
}