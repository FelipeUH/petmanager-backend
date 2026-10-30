package com.codefactory.petmanager.g12.petmanager_backend.common.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.codefactory.petmanager.g12.petmanager_backend.common.exceptions.dto.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
        assertEquals(status, response.getStatusCode());
        assertEquals(status.value(), response.getBody().getCode());
        assertEquals(message, response.getBody().getMessage());
    }

    private static MethodArgumentNotValidException validationException(BindingResult bindingResult) {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        return ex;
    }

    @Test
    void handleEntityNotFound_returns404WithMessage() {
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("no existe")),
            HttpStatus.NOT_FOUND, "no existe");
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("duplicado")),
            HttpStatus.BAD_REQUEST, "duplicado");
    }

    @Test
    void handleAuthorizationDenied_returns403WithFixedMessage() {
        assertError(handler.handleAuthorizationDenied(new AuthorizationDeniedException("Access Denied")),
            HttpStatus.FORBIDDEN, "No tienes los permisos suficientes");
    }

    @Test
    void handleValidationException_joinsDistinctFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "nit", "es requerido"));
        bindingResult.addError(new FieldError("request", "nit", "es requerido"));
        bindingResult.addError(new FieldError("request", "name", "es muy corto"));

        assertError(handler.handleValidationException(validationException(bindingResult)),
            HttpStatus.BAD_REQUEST, "nit: es requerido; name: es muy corto");
    }

    @Test
    void handleValidationException_whenNoFieldErrors_returnsGenericMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        assertError(handler.handleValidationException(validationException(bindingResult)),
            HttpStatus.BAD_REQUEST, "Los datos proporcionados no son válidos");
    }

    @Test
    void handleMissingBody_returns400WithFixedMessage() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("sin cuerpo", new MockHttpInputMessage(new byte[0]));

        assertError(handler.handleMissingBody(ex), HttpStatus.BAD_REQUEST, "Se requiere el cuerpo de la petición");
    }

    @Test
    void handleMissingParameter_returns400NamingTheParameter() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("productId", "int");

        assertError(handler.handleMissingParameter(ex), HttpStatus.BAD_REQUEST, "El parámetro 'productId' es requerido");
    }

    @Test
    void handleTypeMismatch_returns400NamingParameterAndValue() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        when(ex.getValue()).thenReturn("abc");

        assertError(handler.handleTypeMismatch(ex), HttpStatus.BAD_REQUEST, "El parámetro 'id' tiene un valor inválido: abc");
    }

    @Test
    void handleDisabledException_returns403WithMessage() {
        assertError(handler.handleDisabledException(new DisabledException("desactivada")),
            HttpStatus.FORBIDDEN, "desactivada");
    }

    @Test
    void handleBadCredentials_returns401WithMessage() {
        assertError(handler.handleBadCredentials(new BadCredentialsException("inválidas")),
            HttpStatus.UNAUTHORIZED, "inválidas");
    }

    @Test
    void handleLockedException_returns423WithMessage() {
        assertError(handler.handleLockedException(new LockedException("bloqueada")),
            HttpStatus.LOCKED, "bloqueada");
    }

    @Test
    void handleGenericException_returns500WithMessage() {
        assertError(handler.handleGenericException(new RuntimeException("fallo")),
            HttpStatus.INTERNAL_SERVER_ERROR, "fallo");
    }
}
