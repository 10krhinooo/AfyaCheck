package com.kimanga.afyacheck.controllers.api;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.DTO.admin.UserDTO;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.RiskAssessment;
import com.kimanga.afyacheck.repository.RiskAssessmentRepository;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.ServiceHealthMonitor;
import com.kimanga.afyacheck.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Access is restricted to ROLE_ADMIN by ApiSecurityConfig's URL-pattern
// match on /api/admin/**, not by a method-level annotation here (this
// codebase doesn't enable @EnableMethodSecurity, so @PreAuthorize would
// be silently inert and misleading about what's actually enforcing it).
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminService adminService;
    private final UserService userService;
    private final ServiceHealthMonitor serviceHealthMonitor;
    private final RiskAssessmentRepository riskAssessmentRepository;

    public AdminApiController(AdminService adminService, UserService userService,
                               ServiceHealthMonitor serviceHealthMonitor,
                               RiskAssessmentRepository riskAssessmentRepository) {
        this.adminService = adminService;
        this.userService = userService;
        this.serviceHealthMonitor = serviceHealthMonitor;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        DashboardStats stats = adminService.getDashboardStats();

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        response.put("userGrowth", adminService.getUserGrowthData());
        response.put("answerCompletions", adminService.getAnswerCompletionsData());
        response.put("questionTypeDistribution", adminService.getQuestionTypeDistributionData());
        response.put("sectionDistribution", adminService.getSectionDistributionData());
        response.put("riskDistribution", adminService.getRiskDistributionData());
        response.put("recentUsers", adminService.getRecentUsers(10));
        response.put("serviceHealth", serviceHealthMonitor.getHealth());
        return response;
    }

    @GetMapping("/users")
    public List<UserDTO> users() {
        return adminService.getAllUsers();
    }

    @PostMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        ServiceResult<Void> result = adminService.toggleUserStatus(id);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", result.getMessage()));
    }

    @PostMapping("/make-admin")
    public ResponseEntity<?> makeAdmin(@RequestBody Map<String, String> body) {
        ServiceResult<Void> result = userService.makeAdmin(body.get("email"));
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message", result.getMessage()));
    }

    @GetMapping("/questions")
    public Map<String, Object> questions() {
        Map<String, Object> response = new HashMap<>();
        response.put("questions", adminService.getAllQuestions());
        response.put("statistics", adminService.getAnswerStatistics());
        return response;
    }

    @PostMapping("/questions")
    public ResponseEntity<?> addQuestion(@RequestBody Question question) {
        ServiceResult<Question> result = adminService.addQuestion(question);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.getData());
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        ServiceResult<Question> result = adminService.updateQuestion(id, question);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.ok(result.getData());
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        ServiceResult<Void> result = adminService.deleteQuestion(id);
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", result.getMessage()));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"assessments.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("session_id,user_email,risk_level,risk_score,recommendations,created_at");

        for (RiskAssessment assessment : riskAssessmentRepository.findAllForExport()) {
            String sessionId = assessment.getSession().getSessionId();
            String userEmail = assessment.getSession().getUser() != null
                    ? assessment.getSession().getUser().getEmail() : "";
            String recommendations = assessment.getRecommendations() != null
                    ? String.join("; ", assessment.getRecommendations()) : "";

            writer.println(String.join(",",
                    csvEscape(sessionId),
                    csvEscape(userEmail),
                    csvEscape(assessment.getRiskLevel()),
                    csvEscape(String.valueOf(assessment.getRiskScore())),
                    csvEscape(recommendations),
                    csvEscape(String.valueOf(assessment.getCreatedAt()))
            ));
        }

        writer.flush();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
