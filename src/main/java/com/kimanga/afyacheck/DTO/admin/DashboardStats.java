package com.kimanga.afyacheck.DTO.admin;

import lombok.Data;

@Data
public class DashboardStats {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalQuestionnaires; // Now represents total answers
    private Long newUsersThisMonth;
    private Long totalQuestions;
}