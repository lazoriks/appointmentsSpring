package com.example.appointments.dto;

import lombok.Getter;

@Getter
public class ServiceShortDTO {
    private Integer id;
    private String serviceName;
    private Double price;
    private Integer period;
    private String description;

    public ServiceShortDTO(Integer id, String serviceName, Double price, Integer period , String description) {
        this.id = id;
        this.serviceName = serviceName;
        this.price = price;
        this.period = period;
        this.description = description;
    }

}
