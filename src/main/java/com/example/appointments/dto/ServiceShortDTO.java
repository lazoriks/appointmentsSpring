package com.example.appointments.dto;

import lombok.Getter;

@Getter
public class ServiceShortDTO {
    private Integer id;
    private String serviceName;
    private Double price;
    private Integer period; // один час, як у твоїй БД

    public ServiceShortDTO(Integer id, String serviceName, Double price, Integer period) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.period = period;
    }

}
