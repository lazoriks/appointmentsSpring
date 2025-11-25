package com.example.appointments.repository;

import com.example.appointments.entity.Appointment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("""
        SELECT a FROM Appointment a
        JOIN a.services s
        WHERE a.master.id = :masterId
          AND a.datatime BETWEEN :start AND :end
          AND s.id IN :serviceIds
    """)
    List<Appointment> findConflictingAppointments(
            @Param("masterId") Integer masterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("serviceIds") List<Integer> serviceIds
    );

    List<Appointment> findByMasterIdAndDatatimeBetween(Integer masterId, LocalDateTime now, LocalDateTime endDate);

    // 🔽 Додай цей метод
    List<Appointment> findByDatatimeBetween(LocalDateTime from, LocalDateTime to);

    // NEW
    List<Appointment> findByClientIdOrderByDatatimeDesc(Integer clientId);
}
