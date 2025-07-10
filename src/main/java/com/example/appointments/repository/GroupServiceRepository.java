package com.example.appointments.repository;

import com.example.appointments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
//import java.util.Map;

@Repository
public interface GroupServiceRepository extends JpaRepository<GroupService, Integer> {
    List<GroupService> findAll();
}
