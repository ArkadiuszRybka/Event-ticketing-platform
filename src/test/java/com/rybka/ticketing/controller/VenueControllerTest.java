package com.rybka.ticketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rybka.ticketing.config.JwtAuthenticationFilter;
import com.rybka.ticketing.model.dto.admin.VenueCreateDto;
import com.rybka.ticketing.model.dto.admin.VenueReadDto;
import com.rybka.ticketing.model.dto.admin.VenueUpdateDto;
import com.rybka.ticketing.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = VenueController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class VenueControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @MockitoBean
    VenueService venueService;

    private VenueReadDto sample(Long id){
        return new VenueReadDto(id, "Main Hall", 10, 20, 200, LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/admin/venues -> 201 Created")
    void create_ok() throws Exception{
        var in = new VenueCreateDto("Main Hall", 10, 20);
        Mockito.when(venueService.create(any())).thenReturn(sample(1L));

        mvc.perform(post("/api/admin/venues")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(in)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location","/api/admin/venues/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Main Hall"))
                .andExpect(jsonPath("$.totalSeats").value(200));
    }

    @Test
    @DisplayName("Post /api/admin/venues -> 409 Conflict")
    void create_Conflict() throws Exception{
        var in = new VenueCreateDto("Main Hall", 10, 20);
        Mockito.when(venueService.create(any())).thenThrow(
                new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists")
        );

        mvc.perform(post("/api/admin/venues")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(in)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Post /api/admin/venues -> 400 Bad Request")
    void create_badRequest_validation() throws Exception{
        var in = new VenueCreateDto("Main Hall", 0, 20);

        mvc.perform(post("/api/admin/venues")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(in)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Get /api/admin/venues -> 200")
    void list_ok() throws Exception {
        var page = new PageImpl<>(List.of(sample(1L), sample(2L)), PageRequest.of(0, 10), 2);
        Mockito.when(venueService.getAll(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/admin/venues")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @DisplayName("Get /api/admin/venues/{id} -> 200")
    void get_ok() throws Exception {
        Mockito.when(venueService.getById(1L)).thenReturn(sample(1L));

        mvc.perform(get("/api/admin/venues/1")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Get /api/admin/venues/{id} -> 404 Not Found")
    void get_notFound() throws Exception {
        Mockito.when(venueService.getById(1L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));

        mvc.perform(get("/api/admin/venues/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Patch /api/admin/venues/{id} -> 200")
    void update_ok() throws Exception{
        var upd = new VenueUpdateDto("New Name", null, null);
        Mockito.when(venueService.update(eq(1L), any(VenueUpdateDto.class)))
                .thenReturn(new VenueReadDto(1L, "New Name", 10, 20, 200, LocalDateTime.now()));

        mvc.perform(patch("/api/admin/venues/1")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @DisplayName("Patch /api/admin/venues/{id} -> 404 Not Found")
    void update_notFound() throws Exception{
        var upd = new VenueUpdateDto("Some Name", null, null);
        Mockito.when(venueService.update(eq(999L), any(VenueUpdateDto.class))).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));

        mvc.perform(patch("/api/admin/venues/999")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(upd)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Patch /api/admin/venues/{id} -> 404 Not Found")
    void update_conflict() throws Exception{
        var upd = new VenueUpdateDto("Existing", null, null);
        Mockito.when(venueService.update(eq(1L), any(VenueUpdateDto.class))).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Venue name already exists"));

        mvc.perform(patch("/api/admin/venues/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/admin/venues/{id} -> 204 bez treści")
    void delete_noContent() throws Exception {
        mvc.perform(delete("/api/admin/venues/1")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isNoContent());
        Mockito.verify(venueService).delete(1L);
    }
    

    @Test
    @DisplayName("POST /api/admin/venues -> 401 without authorization")
    void create_unauthorized_noAuth() throws Exception {
        var in = new VenueCreateDto("Main Hall", 10, 20);
        mvc.perform(post("/api/admin/venues")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(in)))
                .andExpect(status().isUnauthorized());
    }

}