package com.kimanga.afyacheck.controllers.admin;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.model.UserRole;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    void dashboardReturnsStats() {
        stubDashboardData();

        ResponseEntity<?> response = controller.dashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (AdminController.DashboardResponse) response.getBody();
        assertThat(body.totalUsers()).isEqualTo(10L);
    }

    @Test
    void dashboardReturns500OnException() {
        when(adminService.getDashboardStats()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.dashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void usersReturnsAllUsers() {
        when(adminService.getAllUsers()).thenReturn(List.of());
        when(adminService.getDashboardStats()).thenReturn(stats());
        when(adminService.getAdminUsersCount()).thenReturn(3L);

        ResponseEntity<?> response = controller.users();

        var body = (AdminController.UsersResponse) response.getBody();
        assertThat(body.adminUsersCount()).isEqualTo(3L);
    }

    @Test
    void usersReturns500OnException() {
        when(adminService.getAllUsers()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.users();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void toggleUserStatusReturnsOkOnSuccess() {
        when(adminService.toggleUserStatus(1L)).thenReturn(ServiceResult.success("ok", null));

        ResponseEntity<?> response = controller.toggleUserStatus(new AdminController.ToggleStatusRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void toggleUserStatusReturnsBadRequestOnFailureResult() {
        when(adminService.toggleUserStatus(1L)).thenReturn(ServiceResult.failure("nope"));

        ResponseEntity<?> response = controller.toggleUserStatus(new AdminController.ToggleStatusRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void toggleUserStatusReturns500OnException() {
        when(adminService.toggleUserStatus(1L)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.toggleUserStatus(new AdminController.ToggleStatusRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void questionsReturnsAllQuestions() {
        when(adminService.getAllQuestions()).thenReturn(List.of(new Question()));
        when(adminService.getAnswerStatistics()).thenReturn(Map.of());

        ResponseEntity<?> response = controller.questions();

        var body = (AdminController.QuestionsResponse) response.getBody();
        assertThat(body.questions()).hasSize(1);
    }

    @Test
    void questionsReturns500OnException() {
        when(adminService.getAllQuestions()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.questions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void addQuestionReturnsOkOnSuccess() {
        when(adminService.addQuestion(any())).thenReturn(ServiceResult.success("added", new Question()));

        ResponseEntity<?> response = controller.addQuestion(new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addQuestionReturns500OnException() {
        when(adminService.addQuestion(any())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.addQuestion(new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void addQuestionReturnsBadRequestOnFailureResult() {
        when(adminService.addQuestion(any())).thenReturn(ServiceResult.failure("invalid question"));

        ResponseEntity<?> response = controller.addQuestion(new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteQuestionReturnsOkOnSuccess() {
        when(adminService.deleteQuestion(1L)).thenReturn(ServiceResult.success("deleted", null));

        ResponseEntity<?> response = controller.deleteQuestion(new AdminController.DeleteQuestionRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteQuestionReturns500OnException() {
        when(adminService.deleteQuestion(1L)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.deleteQuestion(new AdminController.DeleteQuestionRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void deleteQuestionReturnsBadRequestOnFailureResult() {
        when(adminService.deleteQuestion(1L)).thenReturn(ServiceResult.failure("not found"));

        ResponseEntity<?> response = controller.deleteQuestion(new AdminController.DeleteQuestionRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateQuestionReturnsOkOnSuccess() {
        when(adminService.updateQuestion(eq(1L), any())).thenReturn(ServiceResult.success("updated", new Question()));

        ResponseEntity<?> response = controller.updateQuestion(1L, new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateQuestionReturns500OnException() {
        when(adminService.updateQuestion(eq(1L), any())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateQuestion(1L, new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void updateQuestionReturnsBadRequestOnFailureResult() {
        when(adminService.updateQuestion(eq(1L), any())).thenReturn(ServiceResult.failure("bad update"));

        ResponseEntity<?> response = controller.updateQuestion(1L, new Question());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void healthCentersReturnsAllCenters() {
        when(adminService.getAllHealthCenters()).thenReturn(List.of(new HealthCenter()));

        ResponseEntity<?> response = controller.healthCenters();

        var body = (AdminController.HealthCentersResponse) response.getBody();
        assertThat(body.healthCenters()).hasSize(1);
    }

    @Test
    void healthCentersReturns500OnException() {
        when(adminService.getAllHealthCenters()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.healthCenters();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void addHealthCenterReturnsOkOnSuccess() {
        when(adminService.addHealthCenter(any())).thenReturn(ServiceResult.success("added", new HealthCenter()));

        ResponseEntity<?> response = controller.addHealthCenter(new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addHealthCenterReturns500OnException() {
        when(adminService.addHealthCenter(any())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.addHealthCenter(new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void addHealthCenterReturnsBadRequestOnFailureResult() {
        when(adminService.addHealthCenter(any())).thenReturn(ServiceResult.failure("invalid center"));

        ResponseEntity<?> response = controller.addHealthCenter(new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteHealthCenterReturnsOkOnSuccess() {
        when(adminService.deleteHealthCenter(1L)).thenReturn(ServiceResult.success("deleted", null));

        ResponseEntity<?> response = controller.deleteHealthCenter(new AdminController.DeleteHealthCenterRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteHealthCenterReturns500OnException() {
        when(adminService.deleteHealthCenter(1L)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.deleteHealthCenter(new AdminController.DeleteHealthCenterRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void deleteHealthCenterReturnsBadRequestOnFailureResult() {
        when(adminService.deleteHealthCenter(1L)).thenReturn(ServiceResult.failure("not found"));

        ResponseEntity<?> response = controller.deleteHealthCenter(new AdminController.DeleteHealthCenterRequest(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateHealthCenterReturnsOkOnSuccess() {
        when(adminService.updateHealthCenter(eq(1L), any())).thenReturn(ServiceResult.success("updated", new HealthCenter()));

        ResponseEntity<?> response = controller.updateHealthCenter(1L, new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateHealthCenterReturns500OnException() {
        when(adminService.updateHealthCenter(eq(1L), any())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateHealthCenter(1L, new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void updateHealthCenterReturnsBadRequestOnFailureResult() {
        when(adminService.updateHealthCenter(eq(1L), any())).thenReturn(ServiceResult.failure("bad update"));

        ResponseEntity<?> response = controller.updateHealthCenter(1L, new HealthCenter());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changeUserRoleSucceeds() {
        when(userService.changeUserRole(1L, UserRole.ADMIN, null))
                .thenReturn(ServiceResult.success("User role updated to ADMIN", null));

        ResponseEntity<?> response =
                controller.changeUserRole(new AdminController.ChangeRoleRequest(1L, UserRole.ADMIN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changeUserRoleHandlesFailure() {
        when(userService.changeUserRole(1L, UserRole.ADMIN, null))
                .thenReturn(ServiceResult.failure("User not found"));

        ResponseEntity<?> response =
                controller.changeUserRole(new AdminController.ChangeRoleRequest(1L, UserRole.ADMIN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
