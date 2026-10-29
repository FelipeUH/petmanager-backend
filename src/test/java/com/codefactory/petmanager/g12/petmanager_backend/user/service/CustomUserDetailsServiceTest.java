package com.codefactory.petmanager.g12.petmanager_backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.codefactory.petmanager.g12.petmanager_backend.auth.model.Role;
import com.codefactory.petmanager.g12.petmanager_backend.user.model.User;
import com.codefactory.petmanager.g12.petmanager_backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_whenUserExists_returnsCustomUserDetails() {
        User user = new User();
        user.setEmail("ana@petmanager.com");
        user.setRole(new Role(1, "ADMIN", "Administrador"));
        when(userRepository.findByEmail("ana@petmanager.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("ana@petmanager.com");

        CustomUserDetails details = assertInstanceOf(CustomUserDetails.class, result);
        assertSame(user, details.getUser());
        assertEquals("ana@petmanager.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_whenUserNotExists_throwsUsernameNotFound() {
        when(userRepository.findByEmail("nadie@petmanager.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername("nadie@petmanager.com"));
    }
}
