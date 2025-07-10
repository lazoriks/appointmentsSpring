package com.example.appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class AvailableDayDto {
    private LocalDate date;
    private List<String> times;
}
