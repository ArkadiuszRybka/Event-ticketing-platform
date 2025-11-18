package com.rybka.ticketing.controller;

import com.rybka.ticketing.config.JwtTokenService;
import com.rybka.ticketing.model.dto.auth.AuthResponse;
import com.rybka.ticketing.model.dto.auth.LoginRequest;
import com.rybka.ticketing.model.dto.auth.MeReadDto;
import com.rybka.ticketing.model.dto.auth.RegisterRequest;
import com.rybka.ticketing.model.entity.User;
import com.rybka.ticketing.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenService jwt;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        User user = userService.register(request.email(), request.password());
        String token = jwt.generateAccessToken(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, "Bearer", jwt.getAccessTtlSec()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        User user = userService.authenticate(request.email(), request.password());
        String token = jwt.generateAccessToken(user);
        return new  AuthResponse(token, "Bearer", jwt.getAccessTtlSec());
    }

    @GetMapping("/me")
    public MeReadDto me(Authentication authentication){
        Long userId = (Long) authentication.getPrincipal();
        User u = userService.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new MeReadDto(u.getId(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
