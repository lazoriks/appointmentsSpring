package com.example.appointments.repository;

import com.example.appointments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    List<Service> findByGroupServiceId(Integer groupId);
}
