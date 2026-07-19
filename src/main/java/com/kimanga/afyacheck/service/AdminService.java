package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.DTO.admin.AuditLogDTO;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.DTO.admin.UserDTO;
import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.repository.*;
import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final HealthCenterRepository healthCenterRepository;
    // Remove sessionRepository dependency since we're not using it

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        try {
            stats.setTotalUsers(userRepository.count());
            stats.setActiveUsers(userRepository.countByEnabledTrue());
            stats.setTotalQuestionnaires(answerRepository.count()); // Count all answers as questionnaire responses
            stats.setTotalQuestions(questionRepository.countByIsActiveTrue());

            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            stats.setNewUsersThisMonth(userRepository.countByCreatedAtAfter(startOfMonth));

        } catch (Exception e) {
            stats.setTotalUsers(0L);
            stats.setActiveUsers(0L);
            stats.setTotalQuestionnaires(0L);
            stats.setTotalQuestions(0L);
            stats.setNewUsersThisMonth(0L);
        }

        return stats;
    }

    // Chart Data Methods
    public Map<String, Object> getUserGrowthData() {
        Map<String, Object> chartData = new HashMap<>();

        try {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<Object[]> results = userRepository.findUserRegistrationsByMonth(sixMonthsAgo);

            List<String> labels = new ArrayList<>();
            List<Long> data = new ArrayList<>();

            // Generate last 6 months labels
            for (int i = 5; i >= 0; i--) {
                LocalDateTime month = LocalDateTime.now().minusMonths(i);
                String label = month.getMonth().toString().substring(0, 3) + " " + month.getYear();
                labels.add(label);

                // Find matching data or set to 0
                Long count = results.stream()
                        .filter(r -> {
                            String resultLabel = r[0].toString();
                            return resultLabel.equals(month.getMonth().toString().substring(0, 3) + " " + month.getYear());
                        })
                        .map(r -> (Long) r[1])
                        .findFirst()
                        .orElse(0L);
                data.add(count);
            }

            chartData.put("labels", labels);
            chartData.put("data", data);

        } catch (Exception e) {
            // Fallback to static data
            chartData.put("labels", Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun"));
            chartData.put("data", Arrays.asList(65L, 78L, 90L, 81L, 86L, 105L));
        }

        return chartData;
    }

    public Map<String, Object> getAnswerCompletionsData() {
        Map<String, Object> chartData = new HashMap<>();

        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            // Get all answers and filter by date
            List<Answer> allAnswers = answerRepository.findAll();
            List<Answer> recentAnswers = allAnswers.stream()
                    .filter(answer -> {
                        if (answer.getCreatedAt() == null) return false;
                        LocalDateTime answerDate = answer.getCreatedAt().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();
                        return answerDate.isAfter(thirtyDaysAgo);
                    })
                    .collect(Collectors.toList());

            // Group by week
            Map<String, Long> weeklyCounts = new LinkedHashMap<>();
            for (int i = 3; i >= 0; i--) {
                LocalDateTime weekStart = LocalDateTime.now().minusWeeks(i + 1);
                LocalDateTime weekEnd = LocalDateTime.now().minusWeeks(i);
                String weekLabel = "Week " + (4 - i);

                long weekCount = recentAnswers.stream()
                        .filter(answer -> {
                            if (answer.getCreatedAt() == null) return false;
                            LocalDateTime answerDate = answer.getCreatedAt().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime();
                            return !answerDate.isBefore(weekStart) && answerDate.isBefore(weekEnd);
                        })
                        .count();

                weeklyCounts.put(weekLabel, weekCount);
            }

            chartData.put("labels", new ArrayList<>(weeklyCounts.keySet()));
            chartData.put("data", new ArrayList<>(weeklyCounts.values()));

        } catch (Exception e) {
            // Fallback to static data
            chartData.put("labels", Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4"));
            chartData.put("data", Arrays.asList(45L, 52L, 68L, 74L));
        }

        return chartData;
    }

    public Map<String, Object> getQuestionTypeDistributionData() {
        Map<String, Object> chartData = new HashMap<>();

        try {
            List<Question> questions = questionRepository.findByIsActiveTrue();

            // Count questions by type
            Map<String, Long> typeCounts = questions.stream()
                    .collect(Collectors.groupingBy(
                            Question::getQuestionType,
                            Collectors.counting()
                    ));

            List<String> labels = new ArrayList<>(typeCounts.keySet());
            List<Long> data = new ArrayList<>(typeCounts.values());
            List<String> backgroundColors = Arrays.asList(
                    "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40"
            );

            chartData.put("labels", labels);
            chartData.put("data", data);
            chartData.put("backgroundColors", backgroundColors.subList(0, Math.min(labels.size(), backgroundColors.size())));

        } catch (Exception e) {
            // Fallback data
            chartData.put("labels", Arrays.asList("yes_no", "multiple_choice", "text", "number", "choice"));
            chartData.put("data", Arrays.asList(45L, 30L, 15L, 8L, 2L));
            chartData.put("backgroundColors", Arrays.asList("#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF"));
        }

        return chartData;
    }

    public Map<String, Object> getSectionDistributionData() {
        Map<String, Object> chartData = new HashMap<>();

        try {
            List<Question> questions = questionRepository.findByIsActiveTrue();

            // Count questions by section
            Map<String, Long> sectionCounts = questions.stream()
                    .collect(Collectors.groupingBy(
                            question -> question.getSectionTitle() != null ? question.getSectionTitle() : "Uncategorized",
                            Collectors.counting()
                    ));

            List<String> labels = new ArrayList<>(sectionCounts.keySet());
            List<Long> data = new ArrayList<>(sectionCounts.values());

            chartData.put("labels", labels);
            chartData.put("data", data);

        } catch (Exception e) {
            // Fallback data
            chartData.put("labels", Arrays.asList("Personal Info", "Symptoms", "Medical History", "Lifestyle"));
            chartData.put("data", Arrays.asList(25L, 35L, 20L, 20L));
        }

        return chartData;
    }

    // User Management Methods
    public List<UserDTO> getRecentUsers(int limit) {
        try {
            return userRepository.findTop10ByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::convertToUserDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<UserDTO> getAllUsers() {
        try {
            return userRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::convertToUserDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public Long getAdminUsersCount() {
        try {
            return userRepository.countByRole(UserRole.ADMIN);
        } catch (Exception e) {
            return 0L;
        }
    }

    public ServiceResult<Void> toggleUserStatus(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ServiceResult.failure("User not found");
            }

            user.setEnabled(!user.getEnabled());
            userRepository.save(user);
            logAction("TOGGLE_USER_STATUS", "USER", String.valueOf(userId), "enabled=" + user.getEnabled());

            String message = user.getEnabled() ? "User activated successfully" : "User deactivated successfully";
            return ServiceResult.success(message, null);

        } catch (Exception e) {
            return ServiceResult.failure("Error updating user status");
        }
    }

    // Question Management Methods
    public List<Question> getAllQuestions() {
        try {
            return questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        } catch (Exception e) {
            return List.of();
        }
    }

    public ServiceResult<Question> addQuestion(Question question) {
        try {
            // Set default values
            if (question.getIsActive() == null) {
                question.setIsActive(true);
            }
            if (question.getDisplayOrder() == null) {
                // Set to max display order + 1
                Integer maxOrder = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                        .map(Question::getDisplayOrder)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);
                question.setDisplayOrder(maxOrder + 1);
            }

            Question savedQuestion = questionRepository.save(question);
            logAction("ADD_QUESTION", "QUESTION", String.valueOf(savedQuestion.getId()), savedQuestion.getQuestionKey());
            return ServiceResult.success("Question added successfully", savedQuestion);
        } catch (Exception e) {
            return ServiceResult.failure("Error adding question: " + e.getMessage());
        }
    }

    public ServiceResult<Void> deleteQuestion(Long questionId) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ServiceResult.failure("Question not found");
            }

            question.setIsActive(false);
            questionRepository.save(question);
            logAction("DELETE_QUESTION", "QUESTION", String.valueOf(questionId), question.getQuestionKey());
            return ServiceResult.success("Question deleted successfully", null);

        } catch (Exception e) {
            return ServiceResult.failure("Error deleting question: " + e.getMessage());
        }
    }

    public ServiceResult<Question> updateQuestion(Long questionId, Question updatedQuestion) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ServiceResult.failure("Question not found");
            }

            question.setQuestionText(updatedQuestion.getQuestionText());
            question.setDescription(updatedQuestion.getDescription());
            question.setQuestionType(updatedQuestion.getQuestionType());
            question.setOptions(updatedQuestion.getOptions());
            question.setMinValue(updatedQuestion.getMinValue());
            question.setMaxValue(updatedQuestion.getMaxValue());
            question.setSectionTitle(updatedQuestion.getSectionTitle());
            question.setDisplayOrder(updatedQuestion.getDisplayOrder());

            Question savedQuestion = questionRepository.save(question);
            logAction("UPDATE_QUESTION", "QUESTION", String.valueOf(questionId), null);
            return ServiceResult.success("Question updated successfully", savedQuestion);

        } catch (Exception e) {
            return ServiceResult.failure("Error updating question: " + e.getMessage());
        }
    }

    public List<HealthCenter> getAllHealthCenters() {
        try {
            return healthCenterRepository.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public ServiceResult<HealthCenter> addHealthCenter(HealthCenter healthCenter) {
        try {
            if (healthCenter.getIsActive() == null) {
                healthCenter.setIsActive(true);
            }
            HealthCenter saved = healthCenterRepository.save(healthCenter);
            logAction("ADD_HEALTH_CENTER", "HEALTH_CENTER", String.valueOf(saved.getId()), saved.getName());
            return ServiceResult.success("Health center added successfully", saved);
        } catch (Exception e) {
            return ServiceResult.failure("Error adding health center: " + e.getMessage());
        }
    }

    public ServiceResult<Void> deleteHealthCenter(Long healthCenterId) {
        try {
            HealthCenter healthCenter = healthCenterRepository.findById(healthCenterId).orElse(null);
            if (healthCenter == null) {
                return ServiceResult.failure("Health center not found");
            }

            healthCenter.setIsActive(false);
            healthCenterRepository.save(healthCenter);
            logAction("DELETE_HEALTH_CENTER", "HEALTH_CENTER", String.valueOf(healthCenterId), healthCenter.getName());
            return ServiceResult.success("Health center deleted successfully", null);
        } catch (Exception e) {
            return ServiceResult.failure("Error deleting health center: " + e.getMessage());
        }
    }

    public ServiceResult<HealthCenter> updateHealthCenter(Long healthCenterId, HealthCenter updated) {
        try {
            HealthCenter healthCenter = healthCenterRepository.findById(healthCenterId).orElse(null);
            if (healthCenter == null) {
                return ServiceResult.failure("Health center not found");
            }

            healthCenter.setName(updated.getName());
            healthCenter.setAddress(updated.getAddress());
            healthCenter.setLatitude(updated.getLatitude());
            healthCenter.setLongitude(updated.getLongitude());
            healthCenter.setPhone(updated.getPhone());
            healthCenter.setHours(updated.getHours());
            healthCenter.setServices(updated.getServices());
            healthCenter.setStiTestingAvailable(updated.getStiTestingAvailable());
            if (updated.getIsActive() != null) {
                healthCenter.setIsActive(updated.getIsActive());
            }

            HealthCenter saved = healthCenterRepository.save(healthCenter);
            logAction("UPDATE_HEALTH_CENTER", "HEALTH_CENTER", String.valueOf(healthCenterId), null);
            return ServiceResult.success("Health center updated successfully", saved);
        } catch (Exception e) {
            return ServiceResult.failure("Error updating health center: " + e.getMessage());
        }
    }

    private void logAction(String action, String targetType, String targetId, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActorEmail(SecurityUtils.currentActorEmail());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        adminAuditLogRepository.save(log);
    }

    public List<AuditLogDTO> getRecentAuditLog() {
        return adminAuditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::convertToAuditLogDTO)
                .collect(Collectors.toList());
    }

    private AuditLogDTO convertToAuditLogDTO(AdminAuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setActorEmail(log.getActorEmail());
        dto.setAction(log.getAction());
        dto.setTargetType(log.getTargetType());
        dto.setTargetId(log.getTargetId());
        dto.setDetails(log.getDetails());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    // Answer Statistics - Simplified without any session dependencies
    public Map<String, Object> getAnswerStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalAnswers = answerRepository.count();
            long totalQuestions = questionRepository.countByIsActiveTrue();

            // Simplified statistics
            stats.put("totalAnswers", totalAnswers);
            stats.put("totalQuestions", totalQuestions);
            stats.put("avgAnswersPerQuestion", totalQuestions > 0 ?
                    String.format("%.2f", (double) totalAnswers / totalQuestions) : "0.00");
            stats.put("completionRate", "N/A"); // Can't calculate without session data

        } catch (Exception e) {
            stats.put("totalAnswers", 0);
            stats.put("totalQuestions", 0);
            stats.put("avgAnswersPerQuestion", "0.00");
            stats.put("completionRate", "N/A");
        }

        return stats;
    }

    // Simplified UserDTO conversion - no session dependencies
    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId().toString());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setJoinDate(user.getCreatedAt());
        dto.setEnabled(user.getEnabled());
        dto.setRole(user.getRole().name());
        dto.setLastActive(user.getUpdatedAt());

        // Set to 0 - we're not counting answers per user
        dto.setQuestionnaireCount(0);

        return dto;
    }
}