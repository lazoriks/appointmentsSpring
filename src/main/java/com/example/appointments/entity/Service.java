package com.example.appointments.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "db_service")
@Data
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @ManyToOne
    @JoinColumn(name = "group_service_id")
    private GroupService groupService;

    private Double price;
    private Integer period;

    @Column(name = "qty_masters")
    private Integer qtyMasters;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(mappedBy = "services")
    private List<Master> masters;

}
