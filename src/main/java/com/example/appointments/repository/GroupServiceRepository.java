package com.example.appointments.repository;

import com.example.appointments.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface GroupServiceRepository extends JpaRepository<GroupService, Integer> {
    //@Query("SELECT DISTINCT new map(g.groupId as id, g.groupName as name) FROM GroupService g")
    //List<Map<String, Object>> findDistinctGroups();

    @Query("SELECT DISTINCT new map(g.id as id, g.groupName as name) FROM GroupService g")
    List<Map<String, Object>> findDistinctGroups();

    List<GroupService> findByGroupId(Integer id);
}
