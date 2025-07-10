package com.example.appointments.repository;

import com.example.appointments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface MasterRepository extends JpaRepository<Master, Integer> {
    List<Master> findByGroupServiceId(Integer groupId);
}

