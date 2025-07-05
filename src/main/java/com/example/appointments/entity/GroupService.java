package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "table_group_service")
@Data
public class GroupService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_name", nullable = false)
    private String groupName;
}
