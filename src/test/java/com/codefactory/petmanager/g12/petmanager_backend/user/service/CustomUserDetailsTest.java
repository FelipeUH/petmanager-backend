package com.codefactory.petmanager.g12.petmanager_backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.codefactory.petmanager.g12.petmanager_backend.auth.model.Role;
import com.codefactory.petmanager.g12.petmanager_backend.user.model.User;

class CustomUserDetailsTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("ana@petmanager.com");
        user.setPassword("hash");
        user.setRole(new Role(2, "MANAGER", "Gerente"));
        user.setActive(true);
    }

    @Test
    void getAuthorities_returnsRoleWithPrefix() {
        CustomUserDetails details = new CustomUserDetails(user);

        List<GrantedAuthority> expected = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
        assertEquals(expected, List.copyOf(details.getAuthorities()));
    }

    @Test
    void getUsernameAndPassword_returnEmailAndPassword() {
        CustomUserDetails details = new CustomUserDetails(user);

        assertEquals("ana@petmanager.com", details.getUsername());
        assertEquals("hash", details.getPassword());
    }

    @Test
    void isEnabled_reflectsUserActiveFlag() {
        assertTrue(new CustomUserDetails(user).isEnabled());

        user.setActive(false);
        assertFalse(new CustomUserDetails(user).isEnabled());
    }

    @Test
    void accountFlags_areAlwaysTrue() {
        CustomUserDetails details = new CustomUserDetails(user);

        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }
}
