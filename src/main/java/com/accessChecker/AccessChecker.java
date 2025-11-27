package com.accessChecker;

import com.enums.Role;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

@Component
public class AccessChecker {

    public void checkUserAccess(Long requestedUserId, Claims claims) {
        Role role = Role.valueOf(claims.get("role", String.class)); // конвертация из JWT
        Long tokenUserId = Long.valueOf(claims.getSubject());

        if (role == Role.ROLE_USER && !tokenUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void checkAdminAccess(Claims claims) {
        Role role = Role.valueOf(claims.get("role", String.class));
        if (role != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
