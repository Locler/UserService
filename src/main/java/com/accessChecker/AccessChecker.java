package com.accessChecker;

import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

@Component
public class AccessChecker {

    public void checkAdminAccess(Set<String> roles) {
        if (roles == null || !roles.contains("ADMIN")) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    public void checkUserAccess(Long targetUserId,
                                Long requesterId,
                                Set<String> roles) {

        if (roles == null) {
            throw new AccessDeniedException("Roles are missing");
        }

        // ADMIN может всё
        if (roles.contains("ADMIN")) {
            return;
        }

        // USER только себя
        if (roles.contains("USER") && targetUserId.equals(requesterId)) {
            return;
        }

        throw new AccessDeniedException("Access denied");
    }
}
