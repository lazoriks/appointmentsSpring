package com.example.appointments.dto;

import lombok.Getter;

@Getter
public class GroupServiceShortDTO {
    private final Integer id;
    private final String groupName;

    public GroupServiceShortDTO(Integer id, String groupName) {
        this.id = id;
        this.groupName = groupName;
    }

}
