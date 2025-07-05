package com.example.appointments.dto;

public class ServiceShortDTO {
    private Integer id;
    private String serviceName;

    public ServiceShortDTO(Integer id, String serviceName) {
        this.id = id;
        this.serviceName = serviceName;
    }

    public Integer getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }
}