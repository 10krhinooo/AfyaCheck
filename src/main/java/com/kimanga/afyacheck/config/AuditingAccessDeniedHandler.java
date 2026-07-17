package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.model.AdminAuditLog;
import com.kimanga.afyacheck.repository.AdminAuditLogRepository;
import com.kimanga.afyacheck.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Records every 403 (authenticated but insufficient role — chiefly non-admins hitting
 * /api/admin/**, the only role-gated prefix in SecurityConfig) to the same admin_audit_log
 * table used for admin actions, so an actual admin can see who's been probing the admin API
 * without permission. Replaces Spring Security's default (a plain 403 with no JSON body,
 * which the React admin console's apiFetch can't parse into a friendly error message).
 */
@Component
public class AuditingAccessDeniedHandler implements AccessDeniedHandler {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AuditingAccessDeniedHandler(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        AdminAuditLog log = new AdminAuditLog();
        log.setActorEmail(SecurityUtils.currentActorEmail());
        log.setAction("ACCESS_DENIED");
        log.setTargetType("ENDPOINT");
        log.setTargetId(request.getMethod() + " " + request.getRequestURI());
        log.setDetails(ex.getMessage());
        adminAuditLogRepository.save(log);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"You don't have permission to access this resource\"}");
    }
}
