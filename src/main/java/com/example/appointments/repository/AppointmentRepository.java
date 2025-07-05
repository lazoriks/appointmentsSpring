package com.example.appointments.repository;

import com.example.appointments.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByDatatimeAndService_Id(LocalDateTime datatime, Integer serviceId);
}