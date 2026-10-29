package com.codefactory.petmanager.g12.petmanager_backend.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.codefactory.petmanager.g12.petmanager_backend.auth.controller.dto.RoleDTO;
import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.GlobalExceptionHandler;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.service.UserService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;

    @InjectMocks private UserController userController;

    private MockMvc mockMvc;
    private RoleDTO adminRole;
    private UserResponseDTO ana;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        adminRole = new RoleDTO(1, "ADMIN", "Administrador");
        ana = new UserResponseDTO(5, "1234567890", "CC", "Ana Gomez", adminRole,
            "3001234567", "ana@petmanager.com", true);
    }

    @Test
    void getAllUsers_returns200WithList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(ana));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("ana@petmanager.com"))
            .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void getUser_whenExists_returns200WithRole() throws Exception {
        when(userService.getUserById(5)).thenReturn(ana);

        mockMvc.perform(get("/api/users/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.role.name").value("ADMIN"));
    }

    @Test
    void getUser_whenNotExists_returns404() throws Exception {
        when(userService.getUserById(999)).thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("No encontrado"));
    }

    @Test
    void changeUserRole_returns200WithUpdatedUser() throws Exception {
        when(userService.changeUserRole(5, 1)).thenReturn(ana);

        mockMvc.perform(patch("/api/users/5/role/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role.id").value(1));
    }

    @Test
    void changeUserRole_whenRoleInvalid_returns400() throws Exception {
        when(userService.changeUserRole(5, 99)).thenThrow(new IllegalArgumentException("role id invalido: 99"));

        mockMvc.perform(patch("/api/users/5/role/99"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("role id invalido: 99"));
    }

    @Test
    void getAllRoles_returns200WithList() throws Exception {
        when(userService.getAllRoles()).thenReturn(List.of(adminRole));

        mockMvc.perform(get("/api/users/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("ADMIN"));
    }

    @Test
    void getRole_whenExists_returns200() throws Exception {
        when(userService.getRoleById(1)).thenReturn(adminRole);

        mockMvc.perform(get("/api/users/roles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Administrador"));
    }

    @Test
    void getRole_whenNotExists_returns404() throws Exception {
        when(userService.getRoleById(99)).thenThrow(new EntityNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/users/roles/99"))
            .andExpect(status().isNotFound());
    }
}
