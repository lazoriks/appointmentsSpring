package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "db_masters")
@Data
public class Master {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name")
    private String firstName;

    private String surname;

    private String mobile;
    private String email;

    @ManyToOne
    @JoinColumn(name = "group_service_id")
    private GroupService groupService;
}
