package com.example.appointments.controller;

import com.example.appointments.dto.HolidayCreateDto;
import com.example.appointments.entity.Holiday;
import com.example.appointments.entity.Master;
import com.example.appointments.repository.HolidayRepository;
import com.example.appointments.repository.MasterRepository;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayRepository holidays;
    private final MasterRepository masters;

    public HolidayController(HolidayRepository holidays, MasterRepository masters) {
        this.holidays = holidays;
        this.masters = masters;
    }

    @GetMapping
    public List<Holiday> all() {
        return holidays.findAll();
    }

    @GetMapping("/master/{id}")
    public List<Holiday> getByMaster(@PathVariable Integer id) {
        return holidays.findByMasterId(id);
    }

    @PostMapping
    public Holiday create(@Valid @RequestBody HolidayCreateDto dto) {
        Master master = masters.findById(dto.masterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Master not found"));

        LocalDate startDate = LocalDate.parse(dto.startDate);
        LocalDate finishDate = LocalDate.parse(dto.finishDate);

        if (finishDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finishDate cannot be before startDate");
        }

        Holiday h = new Holiday();
        h.setMaster(master);
        h.setStartDate(startDate);
        h.setFinishDate(finishDate);

        return holidays.save(h);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        holidays.deleteById(id);
    }
}
