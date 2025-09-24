package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.User;
import com.kimanga.afyacheck.service.UserService;
import com.kimanga.afyacheck.util.AlertMessage;
import com.kimanga.afyacheck.DTO.ServiceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "success", required = false) String success,
                            @RequestParam(value = "resetLinkSent", required = false) String resetLinkSent,
                            @RequestParam(value = "resetSuccess", required = false) String resetSuccess,
                            @RequestParam(value = "verified", required = false) String verified,
                            @RequestParam(value = "invalidToken", required = false) String invalidToken,
                            Model model) {

        if (error != null) model.addAttribute("error", "❌ Invalid email or password.");
        if (logout != null) model.addAttribute("message", "✅ You have been logged out successfully.");
        if (success != null) model.addAttribute("message", AlertMessage.REGISTRATION_SUCCESS);
        if (resetLinkSent != null) model.addAttribute("message", AlertMessage.PASSWORD_RESET_LINK_SENT);
        if (resetSuccess != null) model.addAttribute("message", AlertMessage.PASSWORD_RESET_SUCCESS);
        if (verified != null) model.addAttribute("message", AlertMessage.VERIFICATION_SUCCESS);
        if (invalidToken != null) model.addAttribute("error", AlertMessage.VERIFICATION_FAILED);

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        ServiceResult<User> result = userService.register(user);

        if (!result.isSuccess()) {
            model.addAttribute("errorMessage", result.getMessage());
            return "register";
        }

        return "redirect:/login?success";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        ServiceResult<Void> result = userService.initiatePasswordReset(email);

        if (!result.isSuccess()) {
            model.addAttribute("errorMessage", result.getMessage());
            return "forgot-password";
        }

        return "redirect:/login?resetLinkSent";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model model) {
        ServiceResult<Void> result = userService.resetPassword(token, password);

        if (!result.isSuccess()) {
            model.addAttribute("errorMessage", result.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }

        return "redirect:/login?resetSuccess";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam String token) {
        ServiceResult<Void> result = userService.verifyUser(token);

        if (result.isSuccess()) {
            return "redirect:/login?verified";
        } else {
            return "redirect:/login?invalidToken";
        }
    }

    @GetMapping("/logout-success")
    public String logoutSuccess(Model model) {
        ServiceResult<String> result = userService.logout();
        model.addAttribute("message", result.getData()); // since data = message
        return "login";
    }


}
