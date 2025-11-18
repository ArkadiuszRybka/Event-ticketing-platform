package com.rybka.ticketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import com.rybka.ticketing.config.JwtAuthenticationFilter;
import com.rybka.ticketing.config.JwtTokenService;
import com.rybka.ticketing.config.SecurityConfig;
import com.rybka.ticketing.model.enums.Role;
import com.rybka.ticketing.model.dto.auth.LoginRequest;
import com.rybka.ticketing.model.dto.auth.RegisterRequest;
import com.rybka.ticketing.model.entity.User;
import com.rybka.ticketing.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class AuthControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @MockitoBean
    UserService userService;
    @MockitoBean
    JwtTokenService jwtTokenService;


    private User sampleUser() {
        User u = new User();
        u.setId(1L);
        u.setEmail("user1@example.com");
        u.setPassword("encoded");
        u.setRole(Role.USER);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    @Test
    @DisplayName("POST /api/auth/register -> 201 + token(permitAll)")
    void register_ok() throws Exception{
        User u = sampleUser();
        Mockito.when(userService.register(eq("user1@example.com"), eq("Password123!")))
                .thenReturn(u);
        Mockito.when(jwtTokenService.generateAccessToken(u))
                .thenReturn("token-123");
        Mockito.when(jwtTokenService.getAccessTtlSec())
                .thenReturn(3600L);

        var body = new RegisterRequest("user1@example.com", "Password123!");

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /api/auth/register -> duplicat -> 409")
    void register_conflict() throws Exception{
        Mockito.when(userService.register(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        var body = new RegisterRequest("user1@example.com", "Password123!");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login -> 200 + token")
    void login_ok() throws Exception{
        User u = sampleUser();
        Mockito.when(userService.authenticate("user1@example.com", "Password123!"))
                .thenReturn(u);
        Mockito.when(jwtTokenService.generateAccessToken(u)).thenReturn("token-abc");
        Mockito.when(jwtTokenService.getAccessTtlSec()).thenReturn(3600L);

        var body = new LoginRequest("user1@example.com", "Password123!");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("Post /api/auth/login (bad password) -> 401")
    void login_unathorized() throws Exception{
        Mockito.when(userService.authenticate(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials"));

        var body = new LoginRequest("user1@example.com", "BadPass");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("401 unauthorized"));
    }

    @Test
    @DisplayName("GET /api/auth/me (without Token) -> 401")
    void me_unauth_noToken() throws Exception{
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @DisplayName("GET /api/auth/me (with Token) -> 200 + profil")
    void me_ok_withToken() throws Exception{
        Claims claims = new DefaultClaims();
        claims.setSubject("1");
        claims.put("role", "USER");
        claims.put("email", "user1@example.com");
        Mockito.when(jwtTokenService.parseAndValidate("good-token"))
                        .thenReturn(claims);

        Mockito.when(userService.findById(1L)).thenReturn(Optional.of(sampleUser()));

        mvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}