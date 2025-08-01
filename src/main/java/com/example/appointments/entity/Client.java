package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "db_clients")
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "surname")
    private String surname;

    @Column(nullable = false)
    private String mobile;

    private String email;

    @Column(name = "google_id")
    private String googleId;
}
