package com.shopstack.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collection;
import java.util.List;

public class JwtAuthentication extends AbstractAuthenticationToken {

    private final Long userId;
    private final String email;

    public JwtAuthentication(Long userId, String email, List<String> roles) {
        super(roles.stream()
                .map(r -> (org.springframework.security.core.GrantedAuthority) () -> r)
                .toList());
        this.userId = userId;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }
}
