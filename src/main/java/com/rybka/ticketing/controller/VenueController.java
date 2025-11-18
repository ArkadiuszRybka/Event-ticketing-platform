package com.rybka.ticketing.controller;

import com.rybka.ticketing.model.dto.admin.VenueCreateDto;
import com.rybka.ticketing.model.dto.admin.VenueReadDto;
import com.rybka.ticketing.model.dto.admin.VenueUpdateDto;
import com.rybka.ticketing.service.VenueService;
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
@RequestMapping("/api/admin/venues")
@RequiredArgsConstructor
public class VenueController {
    @Autowired
    private VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueReadDto> create(@Valid @RequestBody VenueCreateDto dto){
        VenueReadDto out = venueService.create(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/admin/venues/" + out.id())
                .body(out);
    }

    @GetMapping("/{id}")
    public VenueReadDto get(@PathVariable Long id){
        return  venueService.getById(id);
    }

    @GetMapping
    public Page<VenueReadDto> list(Pageable pageable){
        return venueService.getAll(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        venueService.delete(id);
    }

    @PatchMapping("/{id}")
    public VenueReadDto update(@PathVariable Long id, @Valid @RequestBody VenueUpdateDto dto){
        return venueService.update(id, dto);
    }
}
