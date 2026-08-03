package com.example.appointments.repository;

import com.example.appointments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupServiceRepository extends JpaRepository<GroupService, Integer> {
}
