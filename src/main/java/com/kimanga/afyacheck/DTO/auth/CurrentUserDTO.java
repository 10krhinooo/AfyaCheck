package com.kimanga.afyacheck.DTO.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CurrentUserDTO {
    private Long id;
    private String email;
    private String username;
    private String name;
    private List<String> roles;
}
