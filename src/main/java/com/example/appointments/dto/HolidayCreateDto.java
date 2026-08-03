package com.example.appointments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class HolidayCreateDto {
    @NotNull(message = "masterId is required")
    public Integer masterId;

    @NotBlank(message = "startDate is required")
    public String startDate;

    @NotBlank(message = "finishDate is required")
    public String finishDate;
}
