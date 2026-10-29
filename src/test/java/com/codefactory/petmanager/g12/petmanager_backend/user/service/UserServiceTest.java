package com.codefactory.petmanager.g12.petmanager_backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codefactory.petmanager.g12.petmanager_backend.auth.controller.dto.RoleDTO;
import com.codefactory.petmanager.g12.petmanager_backend.auth.model.Role;
import com.codefactory.petmanager.g12.petmanager_backend.auth.repository.RoleRepository;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.mapper.UserMapper;
import com.codefactory.petmanager.g12.petmanager_backend.user.model.User;
import com.codefactory.petmanager.g12.petmanager_backend.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private Role employeeRole;
    private Role adminRole;
    private User user;
    private UserRequestDTO request;

    @BeforeEach
    void setUp() {
        employeeRole = new Role(3, "EMPLOYEE", "Empleado");
        adminRole = new Role(1, "ADMIN", "Administrador");

        user = new User();
        user.setId(1);
        user.setEmail("ana@petmanager.com");
        user.setRole(employeeRole);

        request = new UserRequestDTO("1234567890", "CC", "Ana Gomez",
            "3001234567", "ana@petmanager.com", "Secreta123!");
    }

    // ---------- getUserByEmail / getUserById ----------

    @Test
    void getUserByEmail_whenUserExists_returnsDTO() {
        UserResponseDTO expected = new UserResponseDTO();
        when(userRepository.findByEmail("ana@petmanager.com")).thenReturn(Optional.of(user));
        when(userMapper.userToUserResponseDTO(user)).thenReturn(expected);

        assertSame(expected, userService.getUserByEmail("ana@petmanager.com"));
    }

    @Test
    void getUserByEmail_whenUserNotExists_throwsEntityNotFound() {
        when(userRepository.findByEmail("nadie@petmanager.com")).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> userService.getUserByEmail("nadie@petmanager.com"));

        assertTrue(ex.getMessage().contains("nadie@petmanager.com"));
        verifyNoInteractions(userMapper);
    }

    @Test
    void getUserById_whenUserExists_returnsDTO() {
        UserResponseDTO expected = new UserResponseDTO();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userMapper.userToUserResponseDTO(user)).thenReturn(expected);

        assertSame(expected, userService.getUserById(1));
    }

    @Test
    void getUserById_whenUserNotExists_throwsEntityNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> userService.getUserById(999));

        assertTrue(ex.getMessage().contains("999"));
        verifyNoInteractions(userMapper);
    }

    @Test
    void getAllUsers_returnsMappedList() {
        List<User> users = List.of(user);
        List<UserResponseDTO> expected = List.of(new UserResponseDTO());
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.usersToUserResponseDTOs(users)).thenReturn(expected);

        assertSame(expected, userService.getAllUsers());
    }

    // ---------- createUser ----------

    @Test
    void createUser_whenValid_encodesPasswordAndAssignsEmployeeRole() {
        User mapped = new User();
        mapped.setPassword(request.getPassword());
        UserResponseDTO expected = new UserResponseDTO();

        when(userRepository.existsByIdNumberAndIdType("1234567890", "CC")).thenReturn(false);
        when(userRepository.existsByEmail("ana@petmanager.com")).thenReturn(false);
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(userMapper.userRequestDTOToUser(request)).thenReturn(mapped);
        when(passwordEncoder.encode("Secreta123!")).thenReturn("hash");
        when(userRepository.save(mapped)).thenReturn(mapped);
        when(userMapper.userToUserResponseDTO(mapped)).thenReturn(expected);

        UserResponseDTO result = userService.createUser(request);

        assertSame(expected, result);
        assertEquals("hash", mapped.getPassword());
        assertSame(employeeRole, mapped.getRole());
        assertTrue(mapped.isActive());
    }

    @Test
    void createUser_whenDocumentAlreadyExists_throwsIllegalArgument() {
        when(userRepository.existsByIdNumberAndIdType("1234567890", "CC")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> userService.createUser(request));

        assertTrue(ex.getMessage().contains("1234567890"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwsIllegalArgument() {
        when(userRepository.existsByIdNumberAndIdType("1234567890", "CC")).thenReturn(false);
        when(userRepository.existsByEmail("ana@petmanager.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> userService.createUser(request));

        assertTrue(ex.getMessage().contains("ana@petmanager.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_whenEmployeeRoleMissing_throwsInternalError() {
        when(userRepository.existsByIdNumberAndIdType("1234567890", "CC")).thenReturn(false);
        when(userRepository.existsByEmail("ana@petmanager.com")).thenReturn(false);
        when(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.empty());

        assertThrows(InternalError.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // ---------- changeUserRole ----------

    @Test
    void changeUserRole_whenUserAndRoleExist_updatesRole() {
        UserResponseDTO expected = new UserResponseDTO();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponseDTO(user)).thenReturn(expected);

        UserResponseDTO result = userService.changeUserRole(1, 1);

        assertSame(expected, result);
        assertSame(adminRole, user.getRole());
    }

    @Test
    void changeUserRole_whenUserNotExists_throwsEntityNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.changeUserRole(999, 1));

        verifyNoInteractions(roleRepository);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_whenRoleNotExists_throwsIllegalArgument() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> userService.changeUserRole(1, 99));

        assertTrue(ex.getMessage().contains("99"));
        assertSame(employeeRole, user.getRole());
        verify(userRepository, never()).save(any());
    }

    // ---------- roles ----------

    @Test
    void getAllRoles_returnsMappedList() {
        List<Role> roles = List.of(adminRole, employeeRole);
        List<RoleDTO> expected = List.of(new RoleDTO(), new RoleDTO());
        when(roleRepository.findAll()).thenReturn(roles);
        when(userMapper.rolesToRoleDTOs(roles)).thenReturn(expected);

        assertSame(expected, userService.getAllRoles());
    }

    @Test
    void getRoleById_whenRoleExists_returnsDTO() {
        RoleDTO expected = new RoleDTO();
        when(roleRepository.findById(1)).thenReturn(Optional.of(adminRole));
        when(userMapper.roleToRoleDTO(adminRole)).thenReturn(expected);

        assertSame(expected, userService.getRoleById(1));
    }

    @Test
    void getRoleById_whenRoleNotExists_throwsEntityNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
            () -> userService.getRoleById(99));

        assertTrue(ex.getMessage().contains("99"));
        verifyNoInteractions(userMapper);
    }
}
