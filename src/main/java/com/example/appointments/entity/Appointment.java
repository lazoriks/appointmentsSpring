package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "db_appointments")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "datatime", nullable = false)
    private LocalDateTime datatime;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service; // старе поле

    @ManyToMany
    @JoinTable(
            name = "appointment_services",
            joinColumns = @JoinColumn(name = "appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services; // список послуг

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "master_id")
    private Master master;

    @Column(precision = 10, scale = 2)
    private BigDecimal summ;
}
