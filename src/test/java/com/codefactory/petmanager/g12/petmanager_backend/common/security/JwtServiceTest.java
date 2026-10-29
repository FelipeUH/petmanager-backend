package com.codefactory.petmanager.g12.petmanager_backend.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

class JwtServiceTest {

    private static final String SECRET = encode("clave-de-pruebas-de-al-menos-32-bytes!!");
    private static final String OTHER_SECRET = encode("otra-clave-distinta-de-al-menos-32-bytes");

    private JwtService jwtService;

    private static String encode(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private static JwtService newService(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        return service;
    }

    @BeforeEach
    void setUp() {
        jwtService = newService(SECRET, 60_000);
    }

    @Test
    void generateToken_includesSubjectAndRoleClaim() {
        String token = jwtService.generateToken("ana@petmanager.com", "ROLE_ADMIN");

        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
            .build()
            .parseClaimsJws(token)
            .getBody();
        assertEquals("ana@petmanager.com", claims.getSubject());
        assertEquals("ROLE_ADMIN", claims.get("role"));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken("ana@petmanager.com", "ROLE_ADMIN");

        assertEquals("ana@petmanager.com", jwtService.extractUsername(token));
    }

    @Test
    void isValid_whenUsernameMatchesAndNotExpired_returnsTrue() {
        String token = jwtService.generateToken("ana@petmanager.com", "ROLE_ADMIN");

        assertTrue(jwtService.isValid(token, "ana@petmanager.com"));
    }

    @Test
    void isValid_whenUsernameDiffers_returnsFalse() {
        String token = jwtService.generateToken("ana@petmanager.com", "ROLE_ADMIN");

        assertFalse(jwtService.isValid(token, "otro@petmanager.com"));
    }

    @Test
    void extractUsername_whenTokenExpired_throwsExpiredJwt() {
        String expired = newService(SECRET, -1_000).generateToken("ana@petmanager.com", "ROLE_ADMIN");

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(expired));
    }

    @Test
    void extractUsername_whenSignedWithAnotherKey_throwsSignatureException() {
        String foreign = newService(OTHER_SECRET, 60_000).generateToken("ana@petmanager.com", "ROLE_ADMIN");

        assertThrows(SignatureException.class, () -> jwtService.extractUsername(foreign));
    }
}
