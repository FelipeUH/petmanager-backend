package com.codefactory.petmanager.g12.petmanager_backend.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import io.jsonwebtoken.MalformedJwtException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String EMAIL = "ana@petmanager.com";

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        userDetails = User.withUsername(EMAIL).password("hash").roles("ADMIN").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenNoAuthorizationHeader_continuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilter_whenHeaderIsNotBearer_continuesWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilter_whenTokenValid_setsAuthenticationInContext() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.isValid("valid-token", EMAIL)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertSame(userDetails, auth.getPrincipal());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), List.copyOf(auth.getAuthorities()));
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_whenTokenCannotBeParsed_continuesWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer broken-token");
        when(jwtService.extractUsername("broken-token")).thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilter_whenTokenNotValidForUser_continuesWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer stale-token");
        when(jwtService.extractUsername("stale-token")).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.isValid("stale-token", EMAIL)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_whenAlreadyAuthenticated_keepsExistingAuthentication() throws Exception {
        Authentication existing = new TestingAuthenticationToken("otro", null, "ROLE_EMPLOYEE");
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn(EMAIL);

        filter.doFilter(request, response, chain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }
}
