package com.kimanga.afyacheck.controllers.admin;

import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.DTO.admin.UserDTO;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.UserService;
import com.kimanga.afyacheck.DTO.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        loadDashboardData(model);
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "admin-dashboard";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public String usersManagement(Model model) {
        try {
            List<UserDTO> allUsers = adminService.getAllUsers();
            Long totalUsers = adminService.getDashboardStats().getTotalUsers();
            Long adminUsersCount = adminService.getAdminUsersCount();

            model.addAttribute("users", allUsers);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("adminUsersCount", adminUsersCount);
            model.addAttribute("pageTitle", "User Management");
            model.addAttribute("activePage", "users");
        } catch (Exception e) {
            model.addAttribute("users", List.of());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("adminUsersCount", 0);
            model.addAttribute("error", "Unable to load users data");
        }
        return "admin-users";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/toggle-status")
    public String toggleUserStatus(@RequestParam Long userId, Model model) {
        try {
            ServiceResult<Void> result = adminService.toggleUserStatus(userId);
            if (result.isSuccess()) {
                model.addAttribute("successMessage", result.getMessage());
            } else {
                model.addAttribute("errorMessage", result.getMessage());
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating user status");
        }
        return "redirect:/admin/users";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/questions")
    public String questionsManagement(Model model) {
        try {
            List<Question> questions = adminService.getAllQuestions();
            Map<String, Object> answerStats = adminService.getAnswerStatistics();

            model.addAttribute("questions", questions);
            model.addAttribute("answerStats", answerStats);
            model.addAttribute("questionTypes", List.of("yes_no", "multiple_choice", "text", "number", "choice"));
            model.addAttribute("pageTitle", "Question Management");
            model.addAttribute("activePage", "questions");
        } catch (Exception e) {
            model.addAttribute("questions", List.of());
            model.addAttribute("error", "Unable to load questions data");
        }
        return "admin-questions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/add")
    public String addQuestion(@ModelAttribute Question question, Model model) {
        try {
            ServiceResult<Question> result = adminService.addQuestion(question);
            if (result.isSuccess()) {
                model.addAttribute("successMessage", result.getMessage());
            } else {
                model.addAttribute("errorMessage", result.getMessage());
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error adding question");
        }
        return "redirect:/admin/questions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/delete")
    public String deleteQuestion(@RequestParam Long questionId, Model model) {
        try {
            ServiceResult<Void> result = adminService.deleteQuestion(questionId);
            if (result.isSuccess()) {
                model.addAttribute("successMessage", result.getMessage());
            } else {
                model.addAttribute("errorMessage", result.getMessage());
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error deleting question");
        }
        return "redirect:/admin/questions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions/update")
    public String updateQuestion(@RequestParam Long questionId, @ModelAttribute Question question, Model model) {
        try {
            ServiceResult<Question> result = adminService.updateQuestion(questionId, question);
            if (result.isSuccess()) {
                model.addAttribute("successMessage", result.getMessage());
            } else {
                model.addAttribute("errorMessage", result.getMessage());
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating question");
        }
        return "redirect:/admin/questions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/make-admin")
    public String makeUserAdmin(@RequestParam String email, Model model) {
        ServiceResult<Void> result = userService.makeAdmin(email);
        if (result.isSuccess()) {
            model.addAttribute("successMessage", result.getMessage());
        } else {
            model.addAttribute("errorMessage", result.getMessage());
        }

        loadDashboardData(model);
        model.addAttribute("activePage", "dashboard");
        return "admin-dashboard";
    }

    private void loadDashboardData(Model model) {
        try {
            DashboardStats stats = adminService.getDashboardStats();
            model.addAttribute("totalUsers", stats.getTotalUsers() != null ? stats.getTotalUsers() : 0);
            model.addAttribute("activeUsers", stats.getActiveUsers() != null ? stats.getActiveUsers() : 0);
            model.addAttribute("totalQuestionnaires", stats.getTotalQuestionnaires() != null ? stats.getTotalQuestionnaires() : 0);
            model.addAttribute("newUsersThisMonth", stats.getNewUsersThisMonth() != null ? stats.getNewUsersThisMonth() : 0);
            model.addAttribute("totalQuestions", stats.getTotalQuestions() != null ? stats.getTotalQuestions() : 0);

            // Chart data
            model.addAttribute("userGrowthData", adminService.getUserGrowthData());
            model.addAttribute("answerCompletionsData", adminService.getAnswerCompletionsData());
            model.addAttribute("questionTypeDistribution", adminService.getQuestionTypeDistributionData());
            model.addAttribute("sectionDistribution", adminService.getSectionDistributionData());

            List<UserDTO> recentUsers = adminService.getRecentUsers(10);
            model.addAttribute("recentUsers", recentUsers);

        } catch (Exception e) {
            model.addAttribute("totalUsers", 0);
            model.addAttribute("activeUsers", 0);
            model.addAttribute("totalQuestionnaires", 0);
            model.addAttribute("newUsersThisMonth", 0);
            model.addAttribute("totalQuestions", 0);
            model.addAttribute("recentUsers", List.of());
            model.addAttribute("error", "Unable to load dashboard data");
        }
    }
}