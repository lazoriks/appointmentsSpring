package com.example.appointments.dto;

import lombok.Data;

@Data
public class AppointmentCreateDto {
    private String datetime; // ISO формат: "2025-07-11T14:30"
    private Integer serviceId;
    private Integer masterId;

    private String clientName;
    private String clientMobile; // ⬅️ по ньому шукаємо
    private String clientEmail;
}
