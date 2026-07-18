<#--
  Shared HTML wrapper for every Keycloak email (verify-email, password-reset, execute-actions,
  email-update-confirmation, identity-provider-link, event notifications, SMTP test). Each
  message template (see base/email/html/*.ftl, inherited unchanged) calls this same
  <@layout.emailLayout> macro around its body, so branding it once here covers all of them.

  Table-based layout and inline styles throughout, not the login theme's afyacheck.css: email
  clients (Gmail, Outlook desktop) strip <link>/external stylesheets and most @font-face, so
  this can't reuse that theme's approach — falls back to web-safe font stacks that approximate
  the same Fraunces/Inter pairing (see login/resources/css/afyacheck.css for the source palette).
-->
<#macro emailLayout>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>AfyaCheck</title>
<!--[if mso]>
<noscript>
<xml>
<o:OfficeDocumentSettings>
<o:PixelsPerInch>96</o:PixelsPerInch>
</o:OfficeDocumentSettings>
</xml>
</noscript>
<![endif]-->
<style>
  a { color: #256456; }
  p { margin: 0 0 16px 0; }
  p:last-child { margin-bottom: 0; }
</style>
</head>
<body style="margin:0; padding:0; background-color:#faf7f2;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#faf7f2;">
<tr>
<td align="center" style="padding: 32px 16px;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;">
<tr>
<td style="padding-bottom: 24px; text-align: center;">
<a href="${properties.appHomeUrl!'/'}" style="font-family: Georgia, 'Times New Roman', serif; font-weight: 700; font-size: 22px; color: #1c4c41; text-decoration: none;">AfyaCheck</a>
</td>
</tr>
<tr>
<td style="background-color: #ffffff; border-radius: 20px; padding: 32px 28px; box-shadow: 0 1px 2px rgba(16,42,36,0.06), 0 8px 24px -12px rgba(16,42,36,0.18);">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family: Arial, Helvetica, sans-serif; font-size: 15px; line-height: 1.6; color: #1c2521;">
<tr>
<td>
<#nested>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td style="padding-top: 24px; text-align: center; font-family: Arial, Helvetica, sans-serif; font-size: 12px; color: #4a564f;">
AfyaCheck &middot; Confidential HIV/STI risk assessment<br>
This is an automated message &mdash; please don&rsquo;t reply directly to this email.
</td>
</tr>
</table>
</td>
</tr>
</table>
</body>
</html>
</#macro>
