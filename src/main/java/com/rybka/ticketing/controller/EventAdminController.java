package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.admin.CancelRequestDto;
import com.rybka.ticketing.model.dto.admin.EventCreateDto;
import com.rybka.ticketing.model.dto.admin.EventReadDto;
import com.rybka.ticketing.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class EventAdminController {
    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<EventReadDto> create(@Valid @RequestBody EventCreateDto dto){
        EventReadDto out = eventService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/admin/events/" + out.id())
                .body(out);
    }
    @GetMapping
    public Page<EventReadDto> list(Pageable pageable, @RequestParam(required = false) Long venueId){
        return eventService.getAll(pageable, venueId);
    }

    @GetMapping("/{id}")
    public EventReadDto get(@PathVariable Long id){
        return eventService.getById(id);
    }

    @PatchMapping("/{id}/publish")
    public EventReadDto publish(@PathVariable Long id){
        return eventService.publish(id);
    }

    @PatchMapping("/{id}/cancel")
    public EventReadDto cancel(@PathVariable Long id, @RequestBody(required = false) CancelRequestDto body){
        return eventService.cancel(id, body != null ? body.reason() : null);
    }
}
