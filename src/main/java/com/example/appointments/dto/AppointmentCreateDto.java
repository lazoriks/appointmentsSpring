package com.example.appointments.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppointmentCreateDto {
    private String datetime; // ISO формат: "2025-07-11T14:30"
    private Integer serviceId; // зберігається в старе поле
    private List<Integer> serviceIds; // для списку послуг

    private Integer masterId;

    private String clientName;
    private String clientSurname;
    private String clientMobile;
    private String clientEmail;

    private String googleId;
}
