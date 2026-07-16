<#-- Copied from keycloak.v2/login/register.ftl (parent theme), unchanged except the
     brandTitle/brandBody args added to the registrationLayout call below for the split-screen
     panel (see template.ftl). -->
<#import "template.ftl" as layout>
<#import "user-profile-commons.ftl" as userProfileCommons>
<#import "register-commons.ftl" as registerCommons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=true brandTitle="Get started, privately" brandBody="Create an account to keep a history of your assessments. You don't need one just to take the assessment."; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${msg("registerTitle")}
        </#if>
    <#elseif section = "form">
        <style>
            /* Arrangement/spacing for this page, kept local to register.ftl rather than the
               shared afyacheck.css. Also gives every field (whichever macro generated it —
               user-profile-commons.ftl's dynamic attributes, or the password fields below) a
               single, clean bordered box instead of the default PatternFly nesting. */
            #kc-register-form .${properties.kcFormGroupClass!} {
                margin-bottom: 1.75rem;
            }
            #kc-register-form input[type="text"],
            #kc-register-form input[type="email"],
            #kc-register-form input[type="password"] {
                display: block;
                box-sizing: border-box;
                width: 100%;
                padding: 0.65rem 0.85rem;
                border: 1px solid #d3e9e4;
                border-radius: 0.75rem;
                background: #ffffff;
            }
            @media (max-width: 639px) {
                #kc-register-form input[type="text"],
                #kc-register-form input[type="email"],
                #kc-register-form input[type="password"] {
                    padding: 0.75rem 0.85rem;
                }
            }
            #kc-register-form .${properties.kcInputGroup!} {
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            #ac-password-requirements {
                margin: 0.75rem 0 0;
                padding: 0;
                list-style: none;
                display: grid;
                gap: 0.375rem;
            }
            #ac-password-requirements li {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                font-size: 0.8125rem;
                color: #4a564f;
                transition: color 150ms ease;
            }
            #ac-password-requirements .ac-pw-dot {
                width: 0.5rem;
                height: 0.5rem;
                flex-shrink: 0;
                border-radius: 999px;
                background: #d3e9e4;
                transition: background-color 150ms ease;
            }
            #ac-password-requirements li.ac-met {
                color: #1c4c41;
            }
            #ac-password-requirements li.ac-met .ac-pw-dot {
                background: #2f7d6d;
            }
            #ac-password-match {
                display: none;
                margin: 0.5rem 0 0;
                font-size: 0.8125rem;
            }
            #ac-password-match.ac-match {
                display: block;
                color: #1c4c41;
            }
            #ac-password-match.ac-mismatch {
                display: block;
                color: #93392a;
            }
        </style>
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">

            <@userProfileCommons.userProfileFormFields; callback, attribute></@userProfileCommons.userProfileFormFields>

            <#-- Password fields render last, after every other profile field (email, first/last
                 name, etc.) — previously these were injected right after the username/email
                 field via userProfileFormFields' "afterField" callback. -->
            <#if passwordRequired??>
                <div class="${properties.kcFormGroupClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">
                        <span class="pf-v5-c-form__label-text">
                            ${msg("password")}
                            <span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                        </span>
                    </label>
                    <span class="${properties.kcInputGroup!}">
                        <span class="${properties.kcInputClass!}">
                            <input type="password" id="password" name="password"
                                    autocomplete="new-password"
                                    aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                            />
                        </span>
                        <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button" aria-label="${msg('showPassword')}"
                                aria-controls="password" data-password-toggle
                                data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                            <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                        </button>
                    </span>

                    <#if messagesPerField.existsError('password')>
                        <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password'))?no_esc}
                        </span>
                    </#if>

                    <#-- Dynamic, client-side checklist mirroring the realm's password policy —
                         see the script at the bottom of this file. Purely a live UI aid; the
                         realm's actual password policy is still what's enforced on submit. -->
                    <ul id="ac-password-requirements">
                        <li data-rule="length"><span class="ac-pw-dot" aria-hidden="true"></span>At least 8 characters</li>
                        <li data-rule="upper"><span class="ac-pw-dot" aria-hidden="true"></span>One uppercase letter</li>
                        <li data-rule="lower"><span class="ac-pw-dot" aria-hidden="true"></span>One lowercase letter</li>
                        <li data-rule="digit"><span class="ac-pw-dot" aria-hidden="true"></span>One number</li>
                    </ul>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="password-confirm" class="${properties.kcLabelClass!}">
                            <span class="pf-v5-c-form__label-text">
                                ${msg("passwordConfirm")}
                                <span class="pf-v5-c-form__label-required" aria-hidden="true">&#42;</span>
                            </span>
                        </label>
                    </div>
                    <div class="${properties.kcInputGroup!}">
                        <span class="${properties.kcInputClass!}">
                            <input type="password" id="password-confirm"
                                    name="password-confirm"
                                    aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                            />
                        </span>
                        <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button" aria-label="${msg('showPassword')}"
                                aria-controls="password-confirm"  data-password-toggle
                                data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}" data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                            <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                        </button>
                    </div>

                    <#if messagesPerField.existsError('password-confirm')>
                        <span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                        </span>
                    </#if>

                    <p id="ac-password-match" aria-live="polite"></p>
                </div>

                <script>
                    (function () {
                        var password = document.getElementById('password');
                        var confirmPassword = document.getElementById('password-confirm');
                        var matchMessage = document.getElementById('ac-password-match');
                        var rules = {
                            length: function (v) { return v.length >= 8; },
                            upper: function (v) { return /[A-Z]/.test(v); },
                            lower: function (v) { return /[a-z]/.test(v); },
                            digit: function (v) { return /[0-9]/.test(v); }
                        };

                        function updateRequirements() {
                            var value = password.value;
                            Object.keys(rules).forEach(function (rule) {
                                var li = document.querySelector('#ac-password-requirements [data-rule="' + rule + '"]');
                                if (li) li.classList.toggle('ac-met', rules[rule](value));
                            });
                        }

                        function updateMatch() {
                            if (!confirmPassword.value) {
                                matchMessage.className = '';
                                matchMessage.textContent = '';
                                return;
                            }
                            var matches = password.value === confirmPassword.value;
                            matchMessage.className = matches ? 'ac-match' : 'ac-mismatch';
                            matchMessage.textContent = matches ? 'Passwords match' : 'Passwords do not match';
                        }

                        password.addEventListener('input', function () { updateRequirements(); updateMatch(); });
                        confirmPassword.addEventListener('input', updateMatch);
                    })();
                </script>
            </#if>

            <@registerCommons.termsAcceptance/>

            <#if recaptchaRequired??>
                <div class="form-group">
                    <div class="${properties.kcInputWrapperClass!}">
                        <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                    </div>
                </div>
            </#if>

            <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegister")}"/>
            </div>
            <div class="${properties.kcFormGroupClass!} pf-v5-c-login__main-footer-band">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!} pf-v5-c-login__main-footer-band-item">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>
            </div>

        </form>
        <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    </#if>
</@layout.registrationLayout>
