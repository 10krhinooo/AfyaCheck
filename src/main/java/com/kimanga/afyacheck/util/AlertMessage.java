package com.kimanga.afyacheck.util;

public class AlertMessage {

    // ✅ Success messages
    public static final String REGISTRATION_SUCCESS =
            "✅ Account created successfully! Please check your email to verify your account.";
    public static final String VERIFICATION_SUCCESS =
            "✅ Your account has been verified. You can now log in.";
    public static final String PASSWORD_RESET_LINK_SENT =
            "✅ Password reset link has been sent to your email.";
    public static final String PASSWORD_RESET_SUCCESS =
            "✅ Your password has been reset successfully. Please log in with your new password.";

    // ❌ Error messages
    public static final String EMAIL_ALREADY_EXISTS =
            "❌ An account with this email already exists.";
    public static final String VERIFICATION_FAILED =
            " Invalid or expired verification token .";

    public static final String PASSWORD_RESET_FAILED =
           "Invalid or expired password reset token.";
    public static final String EXPIRED_PASSWORD_RESET_FAILED =
            "Expired password reset token.";
    public static final String USER_NOT_FOUND =
            "❌ No user found with the provided email address.";

    private AlertMessage() {
        // Utility class — prevent instantiation
    }
}
