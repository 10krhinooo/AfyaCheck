package com.kimanga.afyacheck.controllers.admin;

import com.kimanga.afyacheck.DTO.admin.AuditLogDTO;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.DTO.admin.UserDTO;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.UserService;
import com.kimanga.afyacheck.DTO.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON API backing the React admin console (see /app/admin). Converted from server-rendered
 * MVC to REST as part of the Thymeleaf -> React migration (Phase 6). @PreAuthorize checks are
 * unchanged — they operate on the Authentication's GrantedAuthority set regardless of whether
 * it came from UserDetails (today) or Keycloak JWT role claims (Phase 7).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    public record DashboardResponse(
            long totalUsers,
            long activeUsers,
            long totalQuestionnaires,
            long newUsersThisMonth,
            long totalQuestions,
            Map<String, Object> userGrowthData,
            Map<String, Object> answerCompletionsData,
            Map<String, Object> questionTypeDistribution,
            Map<String, Object> sectionDistribution,
            List<UserDTO> recentUsers) {}

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        try {
            DashboardStats stats = adminService.getDashboardStats();
            return ResponseEntity.ok(new DashboardResponse(
                    orZero(stats.getTotalUsers()),
                    orZero(stats.getActiveUsers()),
                    orZero(stats.getTotalQuestionnaires()),
                    orZero(stats.getNewUsersThisMonth()),
                    orZero(stats.getTotalQuestions()),
                    adminService.getUserGrowthData(),
                    adminService.getAnswerCompletionsData(),
                    adminService.getQuestionTypeDistributionData(),
                    adminService.getSectionDistributionData(),
                    adminService.getRecentUsers(10)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load dashboard data"));
        }
    }

    private long orZero(Long value) {
        return value != null ? value : 0;
    }

    public record UsersResponse(List<UserDTO> users, long totalUsers, long adminUsersCount) {}

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> users() {
        try {
            List<UserDTO> allUsers = adminService.getAllUsers();
            long totalUsers = orZero(adminService.getDashboardStats().getTotalUsers());
            long adminUsersCount = orZero(adminService.getAdminUsersCount());
            return ResponseEntity.ok(new UsersResponse(allUsers, totalUsers, adminUsersCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load users data"));
        }
    }

    public record ToggleStatusRequest(Long userId) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@RequestBody ToggleStatusRequest request) {
        try {
            ServiceResult<Void> result = adminService.toggleUserStatus(request.userId());
            return result.isSuccess()
                    ? ResponseEntity.ok(Map.of("message", result.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating user status"));
        }
    }

    public record QuestionsResponse(
            List<Question> questions, Map<String, Object> answerStats, List<String> questionTypes) {}

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/questions")
    public ResponseEntity<?> questions() {
        try {
            List<Question> questions = adminService.getAllQuestions();
            Map<String, Object> answerStats = adminService.getAnswerStatistics();
            List<String> questionTypes = List.of("yes_no", "multiple_choice", "text", "number", "choice");
            return ResponseEntity.ok(new QuestionsResponse(questions, answerStats, questionTypes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load questions data"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/add")
    public ResponseEntity<?> addQuestion(@RequestBody Question question) {
        try {
            ServiceResult<Question> result = adminService.addQuestion(question);
            return result.isSuccess()
                    ? ResponseEntity.ok(result.getData())
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error adding question"));
        }
    }

    public record DeleteQuestionRequest(Long questionId) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/delete")
    public ResponseEntity<?> deleteQuestion(@RequestBody DeleteQuestionRequest request) {
        try {
            ServiceResult<Void> result = adminService.deleteQuestion(request.questionId());
            return result.isSuccess()
                    ? ResponseEntity.ok(Map.of("message", result.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting question"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/update")
    public ResponseEntity<?> updateQuestion(@RequestParam Long questionId, @RequestBody Question question) {
        try {
            ServiceResult<Question> result = adminService.updateQuestion(questionId, question);
            return result.isSuccess()
                    ? ResponseEntity.ok(result.getData())
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating question"));
        }
    }

    public record HealthCentersResponse(List<HealthCenter> healthCenters) {}

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/health-centers")
    public ResponseEntity<?> healthCenters() {
        try {
            return ResponseEntity.ok(new HealthCentersResponse(adminService.getAllHealthCenters()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load health centers"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/health-centers/add")
    public ResponseEntity<?> addHealthCenter(@RequestBody HealthCenter healthCenter) {
        try {
            ServiceResult<HealthCenter> result = adminService.addHealthCenter(healthCenter);
            return result.isSuccess()
                    ? ResponseEntity.ok(result.getData())
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error adding health center"));
        }
    }

    public record DeleteHealthCenterRequest(Long healthCenterId) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/health-centers/delete")
    public ResponseEntity<?> deleteHealthCenter(@RequestBody DeleteHealthCenterRequest request) {
        try {
            ServiceResult<Void> result = adminService.deleteHealthCenter(request.healthCenterId());
            return result.isSuccess()
                    ? ResponseEntity.ok(Map.of("message", result.getMessage()))
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting health center"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/health-centers/update")
    public ResponseEntity<?> updateHealthCenter(@RequestParam Long healthCenterId, @RequestBody HealthCenter healthCenter) {
        try {
            ServiceResult<HealthCenter> result = adminService.updateHealthCenter(healthCenterId, healthCenter);
            return result.isSuccess()
                    ? ResponseEntity.ok(result.getData())
                    : ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating health center"));
        }
    }

    public record ChangeRoleRequest(Long userId, UserRole role) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/role")
    public ResponseEntity<?> changeUserRole(@RequestBody ChangeRoleRequest request) {
        ServiceResult<Void> result =
                userService.changeUserRole(request.userId(), request.role(), currentActorKeycloakId());
        Map<String, Object> body = new HashMap<>();
        body.put(result.isSuccess() ? "message" : "error", result.getMessage());
        return result.isSuccess() ? ResponseEntity.ok(body) : ResponseEntity.badRequest().body(body);
    }

    public record AuditLogResponse(List<AuditLogDTO> entries) {}

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit-log")
    public ResponseEntity<?> auditLog() {
        try {
            return ResponseEntity.ok(new AuditLogResponse(adminService.getRecentAuditLog()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to load audit log"));
        }
    }

    private String currentActorKeycloakId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwtAuth ? jwtAuth.getToken().getSubject() : null;
    }
}
