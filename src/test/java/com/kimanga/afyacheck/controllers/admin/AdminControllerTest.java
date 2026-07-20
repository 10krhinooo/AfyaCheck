package com.kimanga.afyacheck.controllers.admin;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.DTO.admin.DashboardStats;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.service.AdminService;
import com.kimanga.afyacheck.service.DecisionTreeClient;
import com.kimanga.afyacheck.service.MLService;
import com.kimanga.afyacheck.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    private final AdminService adminService = mock(AdminService.class);
    private final UserService userService = mock(UserService.class);
    private final MLService mlService = mock(MLService.class);
    private final DecisionTreeClient decisionTreeClient = mock(DecisionTreeClient.class);
    private final AdminController controller = new AdminController(adminService, userService, mlService, decisionTreeClient);

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
    void blacklistedPlacesReturnsOkWithList() {
        when(adminService.getBlacklistedPlaces()).thenReturn(List.of());

        ResponseEntity<?> response = controller.blacklistedPlaces();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = (AdminController.BlacklistedPlacesResponse) response.getBody();
        assertThat(body.blacklistedPlaces()).isEmpty();
    }

    @Test
    void blacklistedPlacesReturns500OnException() {
        when(adminService.getBlacklistedPlaces()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.blacklistedPlaces();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void blacklistPlaceReturnsOkOnSuccess() {
        when(adminService.blacklistPlace("place-1", "Some Clinic"))
                .thenReturn(ServiceResult.success("Health center hidden", new com.kimanga.afyacheck.model.BlacklistedPlace()));

        ResponseEntity<?> response =
                controller.blacklistPlace(new AdminController.BlacklistPlaceRequest("place-1", "Some Clinic"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void blacklistPlaceReturnsBadRequestOnFailureResult() {
        when(adminService.blacklistPlace("place-1", "Some Clinic"))
                .thenReturn(ServiceResult.failure("already hidden"));

        ResponseEntity<?> response =
                controller.blacklistPlace(new AdminController.BlacklistPlaceRequest("place-1", "Some Clinic"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blacklistPlaceReturns500OnException() {
        when(adminService.blacklistPlace(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response =
                controller.blacklistPlace(new AdminController.BlacklistPlaceRequest("place-1", "Some Clinic"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void unblacklistPlaceReturnsOkOnSuccess() {
        when(adminService.unblacklistPlace("place-1")).thenReturn(ServiceResult.success("Health center unhidden", null));

        ResponseEntity<?> response = controller.unblacklistPlace(new AdminController.UnblacklistPlaceRequest("place-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unblacklistPlaceReturnsBadRequestOnFailureResult() {
        when(adminService.unblacklistPlace("place-1")).thenReturn(ServiceResult.failure("not hidden"));

        ResponseEntity<?> response = controller.unblacklistPlace(new AdminController.UnblacklistPlaceRequest("place-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unblacklistPlaceReturns500OnException() {
        when(adminService.unblacklistPlace(anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.unblacklistPlace(new AdminController.UnblacklistPlaceRequest("place-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void promoteToAdminReturnsOkOnSuccess() {
        when(userService.promoteToAdmin(eq("user@example.com"), any()))
                .thenReturn(ServiceResult.success("user@example.com is now an admin", null));

        ResponseEntity<?> response =
                controller.promoteToAdmin(new AdminController.PromoteAdminRequest("user@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void promoteToAdminReturnsBadRequestOnFailureResult() {
        when(userService.promoteToAdmin(anyString(), any()))
                .thenReturn(ServiceResult.failure("No user found with that email"));

        ResponseEntity<?> response =
                controller.promoteToAdmin(new AdminController.PromoteAdminRequest("nobody@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void modelOpsCombinesStatsWithServiceHealth() {
        when(adminService.getModelOpsStats()).thenReturn(Map.of("totalAssessments", 5L));
        when(mlService.isServiceHealthy()).thenReturn(true);
        when(decisionTreeClient.isServiceHealthy()).thenReturn(false);

        ResponseEntity<?> response = controller.modelOps();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        assertThat(body.get("totalAssessments")).isEqualTo(5L);
        assertThat(body.get("mlServiceHealthy")).isEqualTo(true);
        assertThat(body.get("decisionTreeServiceHealthy")).isEqualTo(false);
    }

    @Test
    void modelOpsReturns500OnException() {
        when(adminService.getModelOpsStats()).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.modelOps();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
