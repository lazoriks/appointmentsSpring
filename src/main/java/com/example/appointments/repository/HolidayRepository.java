package com.example.appointments.repository;

import com.example.appointments.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Integer> {

    List<Holiday> findByMasterId(Integer masterId);

    List<Holiday> findByStartDateLessThanEqualAndFinishDateGreaterThanEqual(
            LocalDate finish, LocalDate start
    );
}
