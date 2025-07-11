package com.example.appointments.dto;

public class GroupServiceShortDTO {
    private Integer id;
    private String groupName;

    public GroupServiceShortDTO(Integer id, String groupName) {
        this.id = id;
        this.groupName = groupName;
    }

    public Integer getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }
}
