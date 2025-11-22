package com.kimanga.afyacheck.DTO.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private LocalDateTime joinDate;
    private LocalDateTime lastActive;
    private Boolean enabled;
    private Integer questionnaireCount;
    private String role;

    public void setUsername(String username) {
    }
}