// AfyaCheck Authentication JavaScript - Combined Script

// Navigation Functions
function navigateToLogin() {
    window.location.href = '/login';
}

function navigateToRegister() {
    window.location.href = '/register';
}

function navigateToForgotPassword() {
    window.location.href = '/forgot-password';
}

// Social Login Function
function socialLogin(provider) {
    let message = '';
    switch(provider) {
        case 'google':
            message = 'Google Sign-In would be integrated here';
            break;
        case 'apple':
            message = 'Sign in with Apple would be integrated here';
            break;
        default:
            message = `${provider.charAt(0).toUpperCase() + provider.slice(1)} login would be integrated here`;
    }
    alert(message);
    // Here you would integrate with actual social login providers
    // Example: Google OAuth, Apple Sign-In, etc.
}

// Form Validation Functions
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function validatePassword(password) {
    const requirements = {
        length: password.length >= 8,
        uppercase: /[A-Z]/.test(password),
        lowercase: /[a-z]/.test(password),
        number: /\d/.test(password),
        special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
    };

    return requirements;
}

function validatePasswordsMatch(password, confirmPassword) {
    return password === confirmPassword;
}

// Password Toggle Function
function togglePassword(inputId, toggleBtn) {
    const passwordInput = document.getElementById(inputId);
    if (!passwordInput) return;

    const isPassword = passwordInput.type === 'password';
    passwordInput.type = isPassword ? 'text' : 'password';
    toggleBtn.textContent = isPassword ? '🙈' : '👁️';
}

// Password Requirements Checker
function checkPasswordRequirements(password) {
    const requirements = validatePassword(password);

    // Update requirement indicators
    updateRequirement('lengthReq', requirements.length);
    updateRequirement('uppercaseReq', requirements.uppercase);
    updateRequirement('lowercaseReq', requirements.lowercase);
    updateRequirement('numberReq', requirements.number);
    updateRequirement('specialReq', requirements.special);

    // Calculate strength
    const metRequirements = Object.values(requirements).filter(Boolean).length;
    updatePasswordStrength(metRequirements);

    return Object.values(requirements).every(Boolean);
}

function updateRequirement(elementId, isMet) {
    const element = document.getElementById(elementId);
    if (element) {
        if (isMet) {
            element.classList.add('met');
        } else {
            element.classList.remove('met');
        }
    }
}

function updatePasswordStrength(metRequirements) {
    const strengthBar = document.getElementById('strengthBar');
    const strengthText = document.getElementById('strengthText');

    if (!strengthBar || !strengthText) return;

    // Remove all strength classes
    strengthBar.className = 'password-strength-bar';
    strengthText.className = 'password-strength-text';

    let strengthLevel, strengthLabel;

    switch(metRequirements) {
        case 0:
        case 1:
            strengthLevel = 'weak';
            strengthLabel = 'Very Weak';
            break;
        case 2:
        case 3:
            strengthLevel = 'fair';
            strengthLabel = 'Fair';
            break;
        case 4:
            strengthLevel = 'good';
            strengthLabel = 'Good';
            break;
        case 5:
            strengthLevel = 'strong';
            strengthLabel = 'Strong';
            break;
        default:
            strengthLevel = 'weak';
            strengthLabel = 'Password strength';
    }

    strengthBar.classList.add(strengthLevel);
    strengthText.classList.add(strengthLevel);
    strengthText.textContent = strengthLabel;
}

// Login Form Handler - FIXED: No JavaScript interference for Spring Security
function initializeLogin() {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        console.log('Login form loaded - Spring Security will handle submission');

        // Only add visual enhancements, no form submission handling
        const emailInput = document.getElementById('loginEmail');
        const passwordInput = document.getElementById('loginPassword');

        // Add basic visual feedback
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                const email = this.value.trim();
                if (email && !validateEmail(email)) {
                    this.style.borderColor = '#ef4444';
                } else if (email) {
                    this.style.borderColor = '#10b981';
                }
            });
        }

        // Let Spring Security handle the actual form submission
    }
}

// Register Form Handler - Updated for Original HTML IDs
function initializeRegister() {
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        // Password requirements checker
        const passwordInput = document.getElementById('registerPassword');
        if (passwordInput) {
            passwordInput.addEventListener('input', function() {
                checkPasswordRequirements(this.value);
            });
        }

        // Real-time password confirmation validation
        const confirmPasswordInput = document.getElementById('confirmPassword');
        if (confirmPasswordInput) {
            confirmPasswordInput.addEventListener('input', function() {
                const password = document.getElementById('registerPassword').value;
                const confirmPassword = this.value;

                if (confirmPassword && !validatePasswordsMatch(password, confirmPassword)) {
                    this.style.borderColor = '#ef4444';
                } else if (confirmPassword && validatePasswordsMatch(password, confirmPassword)) {
                    this.style.borderColor = '#10b981';
                } else {
                    this.style.borderColor = 'rgba(148, 163, 184, 0.2)';
                }
            });
        }

        // Email validation on blur
        const emailInput = document.getElementById('email');
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                const email = this.value.trim();
                if (email && !validateEmail(email)) {
                    this.style.borderColor = '#ef4444';
                } else if (email) {
                    this.style.borderColor = '#10b981';
                } else {
                    this.style.borderColor = 'rgba(148, 163, 184, 0.2)';
                }
            });
        }

        // Phone validation (basic)
        const phoneInput = document.getElementById('phone');
        if (phoneInput) {
            phoneInput.addEventListener('blur', function() {
                const phone = this.value.trim();
                // Basic phone validation - at least 10 digits
                const phoneRegex = /^[\+]?[0-9\s\-\(\)]{10,}$/;

                if (phone && !phoneRegex.test(phone)) {
                    this.style.borderColor = '#ef4444';
                } else if (phone) {
                    this.style.borderColor = '#10b981';
                } else {
                    this.style.borderColor = 'rgba(148, 163, 184, 0.2)';
                }
            });
        }

        registerForm.addEventListener('submit', function(e) {
            // Get form values using original HTML IDs
            const name = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const phone = document.getElementById('phone').value;
            const password = document.getElementById('registerPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const agreeTerms = document.getElementById('agreeTerms').checked;

            // Client-side validation
            let hasErrors = false;

            // Validation
            if (!name.trim()) {
                alert('Please enter your full name');
                hasErrors = true;
            }

            if (!validateEmail(email)) {
                alert('Please enter a valid email address');
                hasErrors = true;
            }

            if (!phone.trim()) {
                alert('Please enter your phone number');
                hasErrors = true;
            }

            // Enhanced password validation
            const passwordRequirements = validatePassword(password);
            if (!Object.values(passwordRequirements).every(Boolean)) {
                alert('Password does not meet all requirements. Please check the password requirements below.');
                hasErrors = true;
            }

            if (!validatePasswordsMatch(password, confirmPassword)) {
                alert('Passwords do not match');
                hasErrors = true;
            }

            if (!agreeTerms) {
                alert('Please agree to the Terms & Privacy Policy');
                hasErrors = true;
            }

            // If there are validation errors, prevent form submission
            if (hasErrors) {
                e.preventDefault();
                return false;
            }

            // If validation passes, allow form to submit to Thymeleaf controller
            console.log('Client-side validation passed. Submitting form to server...');
        });
    }
}

// Forgot Password Form Handler - FIXED: Allow normal form submission
function initializeForgotPassword() {
    const forgotPasswordForm = document.getElementById('forgotPasswordForm');
    if (forgotPasswordForm) {
        // Email validation for forgot password
        const emailInput = document.getElementById('forgotEmail');
        if (emailInput) {
            emailInput.addEventListener('blur', function() {
                const email = this.value.trim();
                if (email && !validateEmail(email)) {
                    this.style.borderColor = '#ef4444';
                } else if (email) {
                    this.style.borderColor = '#10b981';
                }
            });
        }

        forgotPasswordForm.addEventListener('submit', function(e) {
            const email = document.getElementById('forgotEmail').value;

            // Basic validation
            if (!validateEmail(email)) {
                alert('Please enter a valid email address');
                e.preventDefault();
                return false;
            }

            // If validation passes, allow normal form submission to Spring Boot controller
            console.log('Forgot password form submitting to server...');

            // Show loading state
            const submitBtn = this.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = 'Sending...';
            submitBtn.disabled = true;

            // Re-enable button after 3 seconds in case of error
            setTimeout(() => {
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }, 3000);
        });
    }
}

// Reset Password Form Handler
function initializeResetPassword() {
    const resetPasswordForm = document.getElementById('resetPasswordForm');
    if (resetPasswordForm) {
        // Password requirements checker
        const newPasswordInput = document.getElementById('newPassword');
        if (newPasswordInput) {
            newPasswordInput.addEventListener('input', function() {
                checkPasswordRequirements(this.value);
            });
        }

        // Real-time password confirmation validation
        const confirmPasswordInput = document.getElementById('confirmPassword');
        if (confirmPasswordInput) {
            confirmPasswordInput.addEventListener('input', function() {
                const password = document.getElementById('newPassword').value;
                const confirmPassword = this.value;

                if (confirmPassword && !validatePasswordsMatch(password, confirmPassword)) {
                    this.style.borderColor = '#ef4444';
                } else if (confirmPassword && validatePasswordsMatch(password, confirmPassword)) {
                    this.style.borderColor = '#10b981';
                }
            });
        }

        resetPasswordForm.addEventListener('submit', function(e) {
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            // Client-side validation
            let hasErrors = false;

            // Enhanced password validation
            const passwordRequirements = validatePassword(newPassword);
            if (!Object.values(passwordRequirements).every(Boolean)) {
                alert('Password does not meet all requirements. Please check the password requirements below.');
                hasErrors = true;
            }

            if (!validatePasswordsMatch(newPassword, confirmPassword)) {
                alert('Passwords do not match');
                hasErrors = true;
            }

            // If there are validation errors, prevent form submission
            if (hasErrors) {
                e.preventDefault();
                return false;
            }

            console.log('Reset password form validation passed. Submitting to server...');
        });
    }
}

// Initialize appropriate form when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('AfyaCheck authentication scripts loaded');

    // Initialize based on available forms
    if (document.getElementById('loginForm')) {
        initializeLogin();
        console.log('Login form initialized - Spring Security will handle submission');
    }

    if (document.getElementById('registerForm')) {
        initializeRegister();
        console.log('Register form initialized');
    }

    if (document.getElementById('forgotPasswordForm')) {
        initializeForgotPassword();
        console.log('Forgot password form initialized');
    }

    if (document.getElementById('resetPasswordForm')) {
        initializeResetPassword();
        console.log('Reset password form initialized');
    }

    // Add smooth loading animation
    document.body.style.opacity = '0';
    setTimeout(() => {
        document.body.style.transition = 'opacity 0.3s ease-in';
        document.body.style.opacity = '1';
    }, 100);

    // Make functions globally available
    window.togglePassword = togglePassword;
    window.validateEmail = validateEmail;
    window.validatePassword = validatePassword;
    window.validatePasswordsMatch = validatePasswordsMatch;
    window.checkPasswordRequirements = checkPasswordRequirements;
    window.socialLogin = socialLogin;
    window.navigateToLogin = navigateToLogin;
    window.navigateToRegister = navigateToRegister;
    window.navigateToForgotPassword = navigateToForgotPassword;
});

// Utility function to show loading state on any form
function showFormLoading(formId, isLoading) {
    const form = document.getElementById(formId);
    if (!form) return;

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
        if (isLoading) {
            submitBtn.dataset.originalText = submitBtn.textContent;
            submitBtn.textContent = 'Processing...';
            submitBtn.disabled = true;
        } else {
            submitBtn.textContent = submitBtn.dataset.originalText || 'Submit';
            submitBtn.disabled = false;
        }
    }
}
document.addEventListener("DOMContentLoaded", function () {
        const alerts = document.querySelectorAll(".alert");
        alerts.forEach(alert => {
            setTimeout(() => {
                alert.classList.add("hide");
            }, 5000); // ⏱ 5 seconds
        });
    });