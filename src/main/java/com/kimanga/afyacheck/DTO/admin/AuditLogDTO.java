package com.kimanga.afyacheck.DTO.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditLogDTO {
    private Long id;
    private String actorEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private LocalDateTime createdAt;
}
