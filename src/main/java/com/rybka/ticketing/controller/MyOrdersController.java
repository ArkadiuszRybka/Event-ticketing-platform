package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.userOrder.MyOrderListItemDto;
import com.rybka.ticketing.model.dto.userOrder.OrderDetailsDto;
import com.rybka.ticketing.model.enums.OrderStatus;
import com.rybka.ticketing.service.MyOrderService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MyOrdersController {
    @Autowired
    private MyOrderService service;

    private static final Set<String> SORT_WHITELIST = Set.of("createdAt", "total", "status");

    @GetMapping("api/me/orders")
    public ResponseEntity<Page<MyOrderListItemDto>> myOrders(
            Authentication auth,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ){
        Long userId = requireUserId(auth);

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid_date_range");
        }

        Set<OrderStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            try {
                statuses = status.stream()
                        .map(s -> OrderStatus.valueOf(s.toUpperCase()))
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(OrderStatus.class)));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "invalid_status");
            }
        }

        Pageable pageable = buildSafePageable(page, size, sort);

        Page<MyOrderListItemDto> result = service.list(userId, statuses, from, to, eventId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/me/orders/{id}")
    public ResponseEntity<OrderDetailsDto> getDetails(
            Authentication auth,
            @PathVariable Long id
    ){
        Long userId = currentUserIdOrNull(auth);
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");

        if(!isAdmin && userId == null){
            throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
        }

        OrderDetailsDto dto = service.getDetails(id, userId, isAdmin);
        return ResponseEntity.ok(dto);
    }


    private Pageable buildSafePageable(int page, int size, String sort) {
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid_size");
        }
        Sort s;
        if (sort == null || sort.isBlank()) {
            s = Sort.by(Sort.Order.desc("createdAt"));
        } else {
            String[] parts = sort.split(",", 2);
            String field = parts[0];
            String dir   = (parts.length > 1 ? parts[1] : "desc").toLowerCase(Locale.ROOT);
            if (!SORT_WHITELIST.contains(field)) {
                throw new ResponseStatusException(BAD_REQUEST, "invalid_sort_field");
            }
            Sort.Order order = "asc".equals(dir) ? Sort.Order.asc(field) : Sort.Order.desc(field);
            s = Sort.by(order);
        }
        return PageRequest.of(page, size, s);
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long uid)) {
            throw new ResponseStatusException(UNAUTHORIZED, "unauthorized");
        }
        return uid;
    }

    private Long currentUserIdOrNull(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long uid) return uid;
        return null;
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
