package com.harunykt.performance_review.security;

import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) { this.user = user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ prefix’i ile ver
        String role = user.getRole() == UserRole.MANAGER ? "ROLE_MANAGER" : "ROLE_EMPLOYEE";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getEmail(); } // email ile login
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public User getDomainUser() { return user; }
}
