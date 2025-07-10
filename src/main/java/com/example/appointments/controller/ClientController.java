package com.example.appointments.controller;

import com.example.appointments.entity.Client;
import com.example.appointments.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "https://glamlimerick.com")
public class ClientController {
    private final ClientRepository repo;

    public ClientController(ClientRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Client> all() {
        return repo.findAll();
    }

    // GET with params to find client by  mobile
    @GetMapping("/search")
    public Map<String, Object> findClientByFirstNameAndMobile(
            //@RequestParam("first_name") String firstName,
            @RequestParam("mobile") String mobile) {

        return repo.findByMobile(mobile)
                .map(client -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", client.getId());
                    return result;
                })
                .orElseGet(HashMap::new);
    }

    // POST with duplicate check + validation
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(@RequestBody Client newClient) {
        Map<String, Object> response = new HashMap<>();

        // validation: mobile cannot be empty
        if (newClient.getMobile() == null || newClient.getMobile().trim().isEmpty()) {
            response.put("error", "Mobile number cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // check for duplicate by mobile
        return repo.findByMobile(newClient.getMobile())
                .map(existingClient -> {
                    response.put("id", existingClient.getId());
                    response.put("message", "Client already exists");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Client savedClient = repo.save(newClient);
                    response.put("id", savedClient.getId());
                    response.put("message", "Client created");
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                });
    }
}