<#-- Copied from base/login/login-verify-email.ftl (parent theme's parent — keycloak.v2 doesn't
     override this one), unchanged except the brandTitle/brandBody args added to the
     registrationLayout call below for the split-screen panel (see template.ftl). -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true brandTitle="Almost there" brandBody="Check your inbox for a verification link to finish setting up your account."; section>
    <#if section = "header">
        ${msg("emailVerifyTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("emailVerifyInstruction1",user.email)}</p>
    <#elseif section = "info">
        <p class="instruction">
            ${msg("emailVerifyInstruction2")}
            <br/>
            <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
        </p>
    </#if>
</@layout.registrationLayout>
