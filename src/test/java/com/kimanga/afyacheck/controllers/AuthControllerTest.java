package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.DTO.ServiceResult;
import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.SessionService;
import com.kimanga.afyacheck.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final UserService userService = mock(UserService.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final AuthController controller = new AuthController(userService, sessionService);

    private HttpSession session() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("http-sid");
        when(sessionService.createOrGetSession("http-sid")).thenReturn("app-sid");
        return session;
    }

    @Test
    void loginPageSetsMessagesForAllParams() {
        Model model = new ExtendedModelMap();
        String view = controller.loginPage("1", "1", "1", "1", "1", "1", "1", "1", session(), model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("error")).isNotNull();
        assertThat(model.getAttribute("oauth")).isNotNull();
    }

    @Test
    void loginPageWithNoParamsHasNoMessages() {
        Model model = new ExtendedModelMap();
        String view = controller.loginPage(null, null, null, null, null, null, null, null, session(), model);

        assertThat(view).isEqualTo("login");
        assertThat(model.getAttribute("error")).isNull();
    }

    @Test
    void loginPageSurvivesSessionCreationFailure() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.loginPage(null, null, null, null, null, null, null, null, session, model);

        assertThat(view).isEqualTo("login");
    }

    @Test
    void registerPageAddsEmptyUserToModel() {
        Model model = new ExtendedModelMap();
        String view = controller.registerPage(model, session());
        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("user")).isInstanceOf(User.class);
    }

    @Test
    void registerPageSurvivesSessionCreationFailure() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.registerPage(model, session);

        assertThat(view).isEqualTo("register");
    }

    @Test
    void registerUserSucceeds() {
        User user = new User();
        when(userService.register(user)).thenReturn(ServiceResult.success("ok", user));
        BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
        Model model = new ExtendedModelMap();

        String view = controller.registerUser(user, bindingResult, model, session());

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("successMessage")).isEqualTo("ok");
    }

    @Test
    void registerUserFailureSetsErrorMessage() {
        User user = new User();
        when(userService.register(user)).thenReturn(ServiceResult.failure("nope"));
        BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
        Model model = new ExtendedModelMap();

        String view = controller.registerUser(user, bindingResult, model, session());

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("nope");
    }

    @Test
    void registerUserHandlesException() {
        User user = new User();
        when(userService.register(user)).thenThrow(new RuntimeException("boom"));
        BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
        Model model = new ExtendedModelMap();

        String view = controller.registerUser(user, bindingResult, model, session());

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("errorMessage")).isNotNull();
    }

    @Test
    void showDashboardRedirectsAdminToAdminDashboard() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Model model = new ExtendedModelMap();

        String view = controller.showDashboard(model, auth);

        assertThat(view).isEqualTo("redirect:/admin/dashboard");
    }

    @Test
    void showDashboardReturnsDashboardForRegularUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Model model = new ExtendedModelMap();

        String view = controller.showDashboard(model, auth);

        assertThat(view).isEqualTo("dashboard");
    }

    @Test
    void showDashboardRedirectsToLoginWhenUnauthenticated() {
        Model model = new ExtendedModelMap();
        String view = controller.showDashboard(model, null);
        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void forgotPasswordPageReturnsView() {
        assertThat(controller.forgotPasswordPage(session())).isEqualTo("forgot-password");
    }

    @Test
    void forgotPasswordPageSurvivesSessionCreationFailure() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));

        assertThat(controller.forgotPasswordPage(session)).isEqualTo("forgot-password");
    }

    @Test
    void processForgotPasswordRedirectsOnSuccess() {
        when(userService.initiatePasswordReset("a@b.com")).thenReturn(ServiceResult.success("sent", null));
        Model model = new ExtendedModelMap();

        String view = controller.processForgotPassword("a@b.com", model, session());

        assertThat(view).isEqualTo("redirect:/login?resetLinkSent");
    }

    @Test
    void processForgotPasswordReturnsFormOnFailure() {
        when(userService.initiatePasswordReset("a@b.com")).thenReturn(ServiceResult.failure("no user"));
        Model model = new ExtendedModelMap();

        String view = controller.processForgotPassword("a@b.com", model, session());

        assertThat(view).isEqualTo("forgot-password");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("no user");
    }

    @Test
    void resetPasswordPageAddsTokenToModel() {
        Model model = new ExtendedModelMap();
        String view = controller.resetPasswordPage("tok", model, session());
        assertThat(view).isEqualTo("reset-password");
        assertThat(model.getAttribute("token")).isEqualTo("tok");
    }

    @Test
    void resetPasswordPageSurvivesSessionCreationFailure() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.resetPasswordPage("tok", model, session);

        assertThat(view).isEqualTo("reset-password");
    }

    @Test
    void resetPasswordRedirectsOnSuccess() {
        when(userService.resetPassword("tok", "newpw")).thenReturn(ServiceResult.success("ok", null));
        Model model = new ExtendedModelMap();

        String view = controller.resetPassword("tok", "newpw", model, session());

        assertThat(view).isEqualTo("redirect:/login?resetSuccess");
    }

    @Test
    void resetPasswordReturnsFormOnFailure() {
        when(userService.resetPassword("tok", "newpw")).thenReturn(ServiceResult.failure("expired"));
        Model model = new ExtendedModelMap();

        String view = controller.resetPassword("tok", "newpw", model, session());

        assertThat(view).isEqualTo("reset-password");
        assertThat(model.getAttribute("token")).isEqualTo("tok");
    }

    @Test
    void verifyUserRedirectsToVerifiedOnSuccess() {
        when(userService.verifyUser("tok")).thenReturn(ServiceResult.success("ok", null));
        String view = controller.verifyUser("tok", session());
        assertThat(view).isEqualTo("redirect:/login?verified");
    }

    @Test
    void verifyUserSurvivesSessionCreationFailure() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));
        when(userService.verifyUser("tok")).thenReturn(ServiceResult.success("ok", null));

        String view = controller.verifyUser("tok", session);

        assertThat(view).isEqualTo("redirect:/login?verified");
    }

    @Test
    void verifyUserRedirectsToInvalidTokenOnFailure() {
        when(userService.verifyUser("tok")).thenReturn(ServiceResult.failure("bad"));
        String view = controller.verifyUser("tok", session());
        assertThat(view).isEqualTo("redirect:/login?invalidToken");
    }
}
