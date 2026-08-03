package com.example.appointments.repository;

import com.example.appointments.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByMasterIdAndDatatimeBetween(Integer masterId, LocalDateTime now, LocalDateTime endDate);

    // Locks the matching rows (SELECT ... FOR UPDATE) so two concurrent booking
    // requests for the same master/day can't both pass the conflict check.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.master.id = :masterId AND a.datatime BETWEEN :start AND :end")
    List<Appointment> lockByMasterIdAndDatatimeBetween(
            @Param("masterId") Integer masterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 🔽 Додай цей метод
    List<Appointment> findByDatatimeBetween(LocalDateTime from, LocalDateTime to);

    // NEW
    List<Appointment> findByClientIdOrderByDatatimeDesc(Integer clientId);
}
