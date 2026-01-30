package com.accessChecker;

import org.springframework.stereotype.Component;


import java.util.Set;

@Component
public class AccessChecker {

    public void checkAdminAccess(Set<String> roles) {

        if (roles.contains("ROLE_ADMIN") || roles.contains("SYSTEM")) {
            return;
        }
        throw new SecurityException("Admin role required");
    }

    // Проверка для сервисного вызова
    public void checkAdminAccess(Set<String> roles, boolean isServiceCall) {
        if (isServiceCall) return; // сервис имеет полный доступ
        checkAdminAccess(roles);
    }

    // Проверка доступа для обычного пользователя или админа
    public void checkUserAccess(Long targetUserId, Long requesterId, Set<String> roles) {
        if (!roles.contains("ROLE_ADMIN") && !targetUserId.equals(requesterId)) {
            throw new SecurityException("Access denied");
        }
    }
}
