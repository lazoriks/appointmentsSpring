package com.example.appointments.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.util.List;

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

    // Needed by master-edit (loads/saves a master's assigned services), so it
    // can't be @JsonIgnore'd like Service.masters. @BatchSize turns the list
    // endpoint's N+1 lazy loads into ~1 batched query instead of one per
    // master (each a cross-region round trip to the eu-north-1 DB).
    @ManyToMany
    @JoinTable(
            name = "db_master_services",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @BatchSize(size = 50)
    private List<Service> services;

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Holiday> holidays;
}
