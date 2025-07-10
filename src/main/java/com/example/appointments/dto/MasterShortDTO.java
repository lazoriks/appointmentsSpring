package com.example.appointments.dto;

public class MasterShortDTO {
    private Integer id;
    private String fullName;

    public MasterShortDTO(Integer id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }
}
