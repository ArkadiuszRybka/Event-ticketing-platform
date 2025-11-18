package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.publics.EventDetailsReadDto;
import com.rybka.ticketing.model.dto.publics.EventPublicReadDto;
import com.rybka.ticketing.service.PublicEventService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PublicEventController {
    @Autowired
    private PublicEventService publicEventService;

    @GetMapping
    public Page<EventPublicReadDto> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long venueId,
            Pageable pageable
    ){
        return publicEventService.listPublished(from, to, venueId, pageable);
    }

    @SneakyThrows
    @GetMapping("/{id}")
    ResponseEntity<EventDetailsReadDto> details(
            @PathVariable Long id,
            @RequestHeader(name = "If-None-Match", required = false) String inm) {
        var dto = publicEventService.getDetails(id);
        String src = publicEventService.buildETagSource(dto);
        String etag = "\"" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(src.getBytes(StandardCharsets.UTF_8))
        ) + "\"";

        if(etag.equals(inm)){
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(dto);
    }
}
