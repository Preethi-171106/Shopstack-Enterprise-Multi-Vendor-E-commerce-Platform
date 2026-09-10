package com.shopstack.security;

import com.shopstack.exception.AccessDeniedException;
import com.shopstack.security.jwt.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    public AuthenticatedUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Not authenticated");
        }
        return user;
    }

    public Long currentUserId() {
        return current().userId();
    }

    public String currentRole() {
        return current().role();
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }

    public void requireRole(String role) {
        if (!hasRole(role)) {
            throw new AccessDeniedException("Access denied: requires role " + role);
        }
    }
}
