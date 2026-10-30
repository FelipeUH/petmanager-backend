package com.codefactory.petmanager.g12.petmanager_backend.auth.service;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codefactory.petmanager.g12.petmanager_backend.common.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private static final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Instant> lastFailedAttempt = new ConcurrentHashMap<>();

    @Value("${auth.login.max-attempts}")
    private int maxAttempts;

    @Value("${auth.login.min-lock-time}")
    private int lockTimeMinutes;

    @Transactional
    public String login(String email, String password) {
        if (isAccountLocked(email)) {
            throw new LockedException("Cuenta bloqueada por exceso de intentos fallidos. Intenta de nuevo más tarde.");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            auth.getPrincipal();
            var role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElseThrow(() -> new InternalError("Rol no encontrado"));

            resetFailedAttempts(email);

            return jwtService.generateToken(email, role);
        } catch (DisabledException ex) {
            throw new DisabledException("La cuenta del usuario está desactivada", ex);
        } catch (BadCredentialsException ex) {
            incrementFailedAttempts(email);
            throw new BadCredentialsException("El email o contraseña son inválidos", ex);
        }
    }

    private void incrementFailedAttempts(String email) {
        failedAttempts.put(email, failedAttempts.getOrDefault(email, 0) + 1);
        lastFailedAttempt.put(email, Instant.now());
    }

    private void resetFailedAttempts(String email) {
        failedAttempts.remove(email);
        lastFailedAttempt.remove(email);
    }

    private boolean isAccountLocked(String email) {
        int attempts = failedAttempts.getOrDefault(email, 0);
        if (attempts < maxAttempts) {
            return false;
        }
        Instant lastAttempt = lastFailedAttempt.get(email);
        if (lastAttempt != null && lastAttempt.plus(lockTimeMinutes, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return true;
        }
        // El bloqueo expiró: la cuenta vuelve a tener todos los intentos disponibles
        resetFailedAttempts(email);
        return false;
    }
}


