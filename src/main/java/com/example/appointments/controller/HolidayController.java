package com.example.appointments.controller;

import com.example.appointments.dto.HolidayCreateDto;
import com.example.appointments.entity.Holiday;
import com.example.appointments.entity.Master;
import com.example.appointments.repository.HolidayRepository;
import com.example.appointments.repository.MasterRepository;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = { "https://glamlimerick.com", "http://localhost:3000" })
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
    public Holiday create(@RequestBody HolidayCreateDto dto) {
        Master master = masters.findById(dto.masterId)
                .orElseThrow(() -> new RuntimeException("Master not found"));

        Holiday h = new Holiday();
        h.setMaster(master);
        h.setStartDate(LocalDate.parse(dto.startDate));
        h.setFinishDate(LocalDate.parse(dto.finishDate));

        return holidays.save(h);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        holidays.deleteById(id);
    }
}
