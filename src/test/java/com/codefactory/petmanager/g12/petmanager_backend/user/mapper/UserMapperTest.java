package com.codefactory.petmanager.g12.petmanager_backend.user.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codefactory.petmanager.g12.petmanager_backend.auth.controller.dto.RoleDTO;
import com.codefactory.petmanager.g12.petmanager_backend.auth.model.Role;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.model.User;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void userToUserResponseDTO_copiesFieldsAndRoleWithoutPassword() {
        Role role = new Role(1, "ADMIN", "Administrador");
        User user = new User(5, "1234567890", "CC", "Ana Gomez", role,
            "3001234567", "ana@petmanager.com", "hash", true);

        UserResponseDTO dto = mapper.userToUserResponseDTO(user);

        assertEquals(new UserResponseDTO(5, "1234567890", "CC", "Ana Gomez",
            new RoleDTO(1, "ADMIN", "Administrador"), "3001234567", "ana@petmanager.com", true), dto);
    }

    @Test
    void userRequestDTOToUser_copiesFieldsAndLeavesRoleAndActiveUnset() {
        UserRequestDTO request = new UserRequestDTO("1234567890", "CC", "Ana Gomez",
            "3001234567", "ana@petmanager.com", "Secreta123!");

        User user = mapper.userRequestDTOToUser(request);

        assertEquals(0, user.getId());
        assertEquals("1234567890", user.getIdNumber());
        assertEquals("CC", user.getIdType());
        assertEquals("Ana Gomez", user.getName());
        assertEquals("3001234567", user.getPhoneNumber());
        assertEquals("ana@petmanager.com", user.getEmail());
        assertEquals("Secreta123!", user.getPassword());
        assertNull(user.getRole());
        assertFalse(user.isActive());
    }

    @Test
    void roleMappings_copyAllFields() {
        Role admin = new Role(1, "ADMIN", "Administrador");
        Role employee = new Role(3, "EMPLOYEE", "Empleado");

        assertEquals(new RoleDTO(1, "ADMIN", "Administrador"), mapper.roleToRoleDTO(admin));
        assertEquals(List.of(new RoleDTO(1, "ADMIN", "Administrador"), new RoleDTO(3, "EMPLOYEE", "Empleado")),
            mapper.rolesToRoleDTOs(List.of(admin, employee)));
    }

    @Test
    void usersToUserResponseDTOs_mapsEveryElement() {
        User first = new User();
        first.setId(1);
        User second = new User();
        second.setId(2);

        List<UserResponseDTO> dtos = mapper.usersToUserResponseDTOs(List.of(first, second));

        assertEquals(List.of(1, 2), dtos.stream().map(UserResponseDTO::getId).toList());
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapper.userToUserResponseDTO(null));
        assertNull(mapper.userRequestDTOToUser(null));
        assertNull(mapper.roleToRoleDTO(null));
        assertNull(mapper.usersToUserResponseDTOs(null));
        assertNull(mapper.rolesToRoleDTOs(null));
    }
}
