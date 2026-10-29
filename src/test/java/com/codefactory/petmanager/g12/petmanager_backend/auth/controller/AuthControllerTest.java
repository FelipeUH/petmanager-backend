package com.codefactory.petmanager.g12.petmanager_backend.auth.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.codefactory.petmanager.g12.petmanager_backend.auth.service.AuthService;
import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.GlobalExceptionHandler;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserRequestDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.controller.dto.UserResponseDTO;
import com.codefactory.petmanager.g12.petmanager_backend.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String LOGIN_JSON = """
        { "email": "ana@petmanager.com", "password": "Secreta123!" }
        """;

    private static final String REGISTER_JSON = """
        {
          "idNumber": "1234567890",
          "idType": "CC",
          "name": "Ana Gomez",
          "phoneNumber": "3001234567",
          "email": "ana@petmanager.com",
          "password": "Secreta123!"
        }
        """;

    @Mock private AuthService authService;
    @Mock private UserService userService;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ---------- login ----------

    @Test
    void login_whenCredentialsValid_returns200WithToken() throws Exception {
        when(authService.login("ana@petmanager.com", "Secreta123!")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void login_whenCredentialsInvalid_returns401() throws Exception {
        when(authService.login("ana@petmanager.com", "Secreta123!"))
            .thenThrow(new BadCredentialsException("El email o contraseña son inválidos"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("El email o contraseña son inválidos"));
    }

    @Test
    void login_whenAccountDisabled_returns403() throws Exception {
        when(authService.login("ana@petmanager.com", "Secreta123!"))
            .thenThrow(new DisabledException("La cuenta del usuario está desactivada"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void login_whenAccountLocked_returns423() throws Exception {
        when(authService.login("ana@petmanager.com", "Secreta123!"))
            .thenThrow(new LockedException("Cuenta bloqueada"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
            .andExpect(status().isLocked());
    }

    // ---------- register ----------

    @Test
    void register_whenValid_returns201() throws Exception {
        UserResponseDTO created = new UserResponseDTO();
        created.setId(5);
        created.setEmail("ana@petmanager.com");
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<UserRequestDTO> captor = ArgumentCaptor.forClass(UserRequestDTO.class);
        verify(userService).createUser(captor.capture());
        assertEquals("1234567890", captor.getValue().getIdNumber());
        assertEquals("Secreta123!", captor.getValue().getPassword());
    }

    @Test
    void register_whenPasswordWeak_returns400WithoutCallingService() throws Exception {
        String weak = REGISTER_JSON.replace("Secreta123!", "123");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(weak))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(startsWith("password:")));

        verifyNoInteractions(userService);
    }

    @Test
    void register_whenEmailDuplicated_returns400() throws Exception {
        when(userService.createUser(any(UserRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Un usuario con el email: ana@petmanager.com ya existe!"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Un usuario con el email: ana@petmanager.com ya existe!"));
    }
}
