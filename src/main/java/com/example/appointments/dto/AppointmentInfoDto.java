package com.example.appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AppointmentInfoDto {
    private Integer appointmentId;
    private Integer clientId;
    private String clientFirstName;
    private String clientMobile;
    private Integer serviceId;
    private LocalDateTime dateCreated;
}