package com.codefactory.petmanager.g12.petmanager_backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.codefactory.petmanager.g12.petmanager_backend.common.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "ana@petmanager.com";
    private static final String PASSWORD = "Secreta123!";
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_MINUTES = 5;

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private Map<String, Integer> failedAttempts;
    private Map<String, Instant> lastFailedAttempt;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(authService, "lockTimeMinutes", LOCK_MINUTES);

        // Los mapas son estáticos: se limpian para que no se comparta estado entre tests
        failedAttempts = (Map<String, Integer>) ReflectionTestUtils.getField(AuthService.class, "failedAttempts");
        lastFailedAttempt = (Map<String, Instant>) ReflectionTestUtils.getField(AuthService.class, "lastFailedAttempt");
        failedAttempts.clear();
        lastFailedAttempt.clear();
    }

    private Authentication authenticatedAs(String role) {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of(new SimpleGrantedAuthority(role)));
    }

    private void failLogin(int times) {
        for (int i = 0; i < times; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(EMAIL, "incorrecta"));
        }
    }

    @Test
    void login_whenCredentialsValid_returnsTokenWithRole() {
        when(authenticationManager.authenticate(any())).thenReturn(authenticatedAs("ROLE_ADMIN"));
        when(jwtService.generateToken(EMAIL, "ROLE_ADMIN")).thenReturn("token");

        String token = authService.login(EMAIL, PASSWORD);

        assertEquals("token", token);
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals(EMAIL, captor.getValue().getPrincipal());
        assertEquals(PASSWORD, captor.getValue().getCredentials());
    }

    @Test
    void login_whenCredentialsInvalid_throwsBadCredentialsAndCountsAttempt() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
            () -> authService.login(EMAIL, "incorrecta"));

        assertEquals("El email o contraseña son inválidos", ex.getMessage());
        assertEquals(1, failedAttempts.get(EMAIL));
        assertTrue(lastFailedAttempt.containsKey(EMAIL));
    }

    @Test
    void login_whenUserDisabled_throwsDisabledWithoutCountingAttempt() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        DisabledException ex = assertThrows(DisabledException.class, () -> authService.login(EMAIL, PASSWORD));

        assertEquals("La cuenta del usuario está desactivada", ex.getMessage());
        assertFalse(failedAttempts.containsKey(EMAIL));
    }

    @Test
    void login_whenUserHasNoAuthorities_throwsInternalError() {
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));

        assertThrows(InternalError.class, () -> authService.login(EMAIL, PASSWORD));

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_whenMaxAttemptsReached_locksAccountWithoutAuthenticating() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        failLogin(MAX_ATTEMPTS);

        assertThrows(LockedException.class, () -> authService.login(EMAIL, PASSWORD));

        verify(authenticationManager, times(MAX_ATTEMPTS)).authenticate(any());
    }

    @Test
    void login_whenBelowMaxAttempts_stillAuthenticates() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"))
            .thenThrow(new BadCredentialsException("bad"))
            .thenReturn(authenticatedAs("ROLE_EMPLOYEE"));
        when(jwtService.generateToken(EMAIL, "ROLE_EMPLOYEE")).thenReturn("token");
        failLogin(MAX_ATTEMPTS - 1);

        assertEquals("token", authService.login(EMAIL, PASSWORD));
    }

    @Test
    void login_whenSuccessful_resetsFailedAttempts() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"))
            .thenReturn(authenticatedAs("ROLE_EMPLOYEE"));
        when(jwtService.generateToken(EMAIL, "ROLE_EMPLOYEE")).thenReturn("token");
        failLogin(1);

        authService.login(EMAIL, PASSWORD);

        assertFalse(failedAttempts.containsKey(EMAIL));
        assertFalse(lastFailedAttempt.containsKey(EMAIL));
    }

    @Test
    void login_whenLockTimeHasPassed_allowsLoginAgain() {
        failedAttempts.put(EMAIL, MAX_ATTEMPTS);
        lastFailedAttempt.put(EMAIL, Instant.now().minus(LOCK_MINUTES + 1, ChronoUnit.MINUTES));
        when(authenticationManager.authenticate(any())).thenReturn(authenticatedAs("ROLE_EMPLOYEE"));
        when(jwtService.generateToken(EMAIL, "ROLE_EMPLOYEE")).thenReturn("token");

        assertEquals("token", authService.login(EMAIL, PASSWORD));
    }

    @Test
    void login_whenAnotherAccountIsLocked_doesNotAffectThisOne() {
        failedAttempts.put("otro@petmanager.com", MAX_ATTEMPTS);
        lastFailedAttempt.put("otro@petmanager.com", Instant.now());
        when(authenticationManager.authenticate(any())).thenReturn(authenticatedAs("ROLE_EMPLOYEE"));
        when(jwtService.generateToken(EMAIL, "ROLE_EMPLOYEE")).thenReturn("token");

        assertEquals("token", authService.login(EMAIL, PASSWORD));
    }
}
