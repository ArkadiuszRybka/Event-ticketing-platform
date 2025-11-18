package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.seat.HoldRequestDto;
import com.rybka.ticketing.model.dto.seat.HoldResponseDto;
import com.rybka.ticketing.service.HoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/events/{eventId}/hold")
@RequiredArgsConstructor
public class HoldController {
    @Autowired
    private HoldService holdService;

    private Long currentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !(auth.getPrincipal() instanceof Long uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return uid;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponseDto hold(@PathVariable Long eventId,
                                @RequestHeader("Idempotency-Key") String idempotencyKey,
                                @RequestBody HoldRequestDto body) {
        Long userId = currentUserId();
        return holdService.hold(eventId, userId, idempotencyKey, body.seats());
    }
}
