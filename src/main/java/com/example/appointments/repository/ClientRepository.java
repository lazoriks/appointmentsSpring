package com.example.appointments.repository;

import com.example.appointments.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {
    Optional<Client> findByMobile(String mobile);
    Optional<Client> findByGoogleId(String googleId);
}