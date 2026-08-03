package com.example.appointments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AppointmentCreateDto {
    @NotBlank(message = "datetime is required")
    private String datetime; // ISO формат: "2025-07-11T14:30"

    @NotNull(message = "serviceId is required")
    private Integer serviceId; // зберігається в старе поле

    @NotEmpty(message = "serviceIds must contain at least one service")
    private List<Integer> serviceIds; // для списку послуг

    @NotNull(message = "masterId is required")
    private Integer masterId;

    @NotBlank(message = "clientName is required")
    private String clientName;
    private String clientSurname;

    @NotBlank(message = "clientMobile is required")
    private String clientMobile;

    @Email(message = "clientEmail must be a valid email")
    private String clientEmail;

    private String googleId;
}
