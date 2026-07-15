package com.kimanga.afyacheck.controllers.admin;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    private final UserService userService = mock(UserService.class);
    private final AdminService adminService = mock(AdminService.class);
    private final AdminController controller = new AdminController(userService, adminService);

    private DashboardStats stats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalUsers(10L);
        stats.setActiveUsers(5L);
        stats.setTotalQuestionnaires(20L);
        stats.setNewUsersThisMonth(2L);
        stats.setTotalQuestions(15L);
        return stats;
    }

    private void stubDashboardData() {
        when(adminService.getDashboardStats()).thenReturn(stats());
        when(adminService.getUserGrowthData()).thenReturn(Map.of());
        when(adminService.getAnswerCompletionsData()).thenReturn(Map.of());
        when(adminService.getQuestionTypeDistributionData()).thenReturn(Map.of());
        when(adminService.getSectionDistributionData()).thenReturn(Map.of());
        when(adminService.getRecentUsers(anyInt())).thenReturn(List.of());
    }

    @Test
    void adminDashboardPopulatesModel() {
        stubDashboardData();
        Model model = new ExtendedModelMap();

        String view = controller.adminDashboard(model);

        assertThat(view).isEqualTo("admin-dashboard");
        assertThat(model.getAttribute("totalUsers")).isEqualTo(10L);
        assertThat(model.getAttribute("pageTitle")).isEqualTo("Admin Dashboard");
    }

    @Test
    void adminDashboardFallsBackOnException() {
        when(adminService.getDashboardStats()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        controller.adminDashboard(model);

        assertThat(model.getAttribute("totalUsers")).isEqualTo(0);
        assertThat(model.getAttribute("error")).isNotNull();
    }

    @Test
    void usersManagementPopulatesModel() {
        when(adminService.getAllUsers()).thenReturn(List.of());
        when(adminService.getDashboardStats()).thenReturn(stats());
        when(adminService.getAdminUsersCount()).thenReturn(3L);

        Model model = new ExtendedModelMap();
        String view = controller.usersManagement(model);

        assertThat(view).isEqualTo("admin-users");
        assertThat(model.getAttribute("adminUsersCount")).isEqualTo(3L);
    }

    @Test
    void usersManagementFallsBackOnException() {
        when(adminService.getAllUsers()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.usersManagement(model);

        assertThat(view).isEqualTo("admin-users");
        assertThat(model.getAttribute("error")).isNotNull();
    }

    @Test
    void toggleUserStatusRedirectsOnSuccess() {
        when(adminService.toggleUserStatus(1L)).thenReturn(ServiceResult.success("ok", null));
        Model model = new ExtendedModelMap();

        String view = controller.toggleUserStatus(1L, model);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(model.getAttribute("successMessage")).isEqualTo("ok");
    }

    @Test
    void toggleUserStatusHandlesFailureResult() {
        when(adminService.toggleUserStatus(1L)).thenReturn(ServiceResult.failure("nope"));
        Model model = new ExtendedModelMap();

        controller.toggleUserStatus(1L, model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("nope");
    }

    @Test
    void toggleUserStatusHandlesException() {
        when(adminService.toggleUserStatus(1L)).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        controller.toggleUserStatus(1L, model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("Error updating user status");
    }

    @Test
    void questionsManagementPopulatesModel() {
        when(adminService.getAllQuestions()).thenReturn(List.of(new Question()));
        when(adminService.getAnswerStatistics()).thenReturn(Map.of());

        Model model = new ExtendedModelMap();
        String view = controller.questionsManagement(model);

        assertThat(view).isEqualTo("admin-questions");
        assertThat((List<?>) model.getAttribute("questions")).hasSize(1);
    }

    @Test
    void questionsManagementFallsBackOnException() {
        when(adminService.getAllQuestions()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.questionsManagement(model);

        assertThat(view).isEqualTo("admin-questions");
        assertThat(model.getAttribute("error")).isNotNull();
    }

    @Test
    void addQuestionRedirects() {
        when(adminService.addQuestion(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ServiceResult.success("added", new Question()));
        Model model = new ExtendedModelMap();

        String view = controller.addQuestion(new Question(), model);

        assertThat(view).isEqualTo("redirect:/admin/questions");
        assertThat(model.getAttribute("successMessage")).isEqualTo("added");
    }

    @Test
    void addQuestionHandlesException() {
        when(adminService.addQuestion(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        controller.addQuestion(new Question(), model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("Error adding question");
    }

    @Test
    void addQuestionSetsErrorMessageOnFailureResult() {
        when(adminService.addQuestion(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ServiceResult.failure("invalid question"));
        Model model = new ExtendedModelMap();

        controller.addQuestion(new Question(), model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("invalid question");
    }

    @Test
    void deleteQuestionRedirects() {
        when(adminService.deleteQuestion(1L)).thenReturn(ServiceResult.success("deleted", null));
        Model model = new ExtendedModelMap();

        String view = controller.deleteQuestion(1L, model);

        assertThat(view).isEqualTo("redirect:/admin/questions");
    }

    @Test
    void deleteQuestionHandlesException() {
        when(adminService.deleteQuestion(1L)).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        controller.deleteQuestion(1L, model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("Error deleting question");
    }

    @Test
    void deleteQuestionSetsErrorMessageOnFailureResult() {
        when(adminService.deleteQuestion(1L)).thenReturn(ServiceResult.failure("not found"));
        Model model = new ExtendedModelMap();

        controller.deleteQuestion(1L, model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("not found");
    }

    @Test
    void updateQuestionRedirects() {
        when(adminService.updateQuestion(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ServiceResult.success("updated", new Question()));
        Model model = new ExtendedModelMap();

        String view = controller.updateQuestion(1L, new Question(), model);

        assertThat(view).isEqualTo("redirect:/admin/questions");
    }

    @Test
    void updateQuestionHandlesException() {
        when(adminService.updateQuestion(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        controller.updateQuestion(1L, new Question(), model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("Error updating question");
    }

    @Test
    void updateQuestionSetsErrorMessageOnFailureResult() {
        when(adminService.updateQuestion(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ServiceResult.failure("bad update"));
        Model model = new ExtendedModelMap();

        controller.updateQuestion(1L, new Question(), model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("bad update");
    }

    @Test
    void makeUserAdminSucceeds() {
        when(userService.makeAdmin("a@b.com")).thenReturn(ServiceResult.success("promoted", null));
        stubDashboardData();
        Model model = new ExtendedModelMap();

        String view = controller.makeUserAdmin("a@b.com", model);

        assertThat(view).isEqualTo("admin-dashboard");
        assertThat(model.getAttribute("successMessage")).isEqualTo("promoted");
    }

    @Test
    void makeUserAdminHandlesFailure() {
        when(userService.makeAdmin("a@b.com")).thenReturn(ServiceResult.failure("not found"));
        stubDashboardData();
        Model model = new ExtendedModelMap();

        controller.makeUserAdmin("a@b.com", model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("not found");
    }
}
