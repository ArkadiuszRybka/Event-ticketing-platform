package com.rybka.ticketing.controller;

import com.rybka.ticketing.config.JwtAuthenticationFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(
        controllers = EventAdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class EventAdminControllerTest {
//    @Autowired
//    MockMvc mvc;
//    @Autowired
//    ObjectMapper om;
//    @MockitoBean
//    EventService eventService;
//
//    private EventReadDto sample(long id) {
//        return new EventReadDto(id, 1L, "Main Hall", "Show",
//                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2),
//                "DRAFT", new BigDecimal("49.99"));
//    }
//
//    @Test
//    @DisplayName("POST /api/admin/events -> 201 Created")
//    void create_ok() throws Exception{
//        var in = new EventCreateDto(1L, "Show",
//                LocalDateTime.now().plusDays(2),
//                LocalDateTime.now().plusDays(2).plusHours(2),
//                new BigDecimal("49.99"));
//
//        Mockito.when(eventService.create(any())).thenReturn(sample(10));
//
//        mvc.perform(post("/api/admin/events")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(om.writeValueAsString(in)))
//                .andExpect(status().isCreated())
//                .andExpect(header().string("Location", "/api/admin/events/10"))
//                .andExpect(jsonPath("$.id").value(10));
//    }
//
//    @Test
//    @DisplayName("POST /api/admin/events -> 400 bad request")
//    void create_badRequest() throws Exception{
//        var invalid = new EventCreateDto(1L, "Show",
//                LocalDateTime.now().plusDays(2),
//                LocalDateTime.now().plusDays(2).plusHours(2),
//                new BigDecimal("0.00"));
//
//        mvc.perform(post("/api/admin/events")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(om.writeValueAsString(invalid)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @DisplayName("GET /api/admin/events -> 200 lista")
//    void list_ok() throws Exception {
//        var page = new PageImpl<>(List.of(sample(1L),sample(2L)), PageRequest.of(0, 10), 2);
//        Mockito.when(eventService.getAll(any(Pageable.class),any())).thenReturn(page);
//
//        mvc.perform(get("/api/admin/events")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.length()").value(2))
//                .andExpect(jsonPath("$.content[0].id").value(1));
//    }
//
//    @Test
//    @DisplayName("GET /api/admin/events/{id} -> 404 not found")
//    void get_notFound() throws Exception {
//        Mockito.when(eventService.getById(eq(999L)))
//                        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
//
//        mvc.perform(get("/api/admin/events/999")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    @DisplayName("PATCH /api/admin/events/{id}/publish -> 200")
//    void publish_ok() throws Exception {
//        Mockito.when(eventService.publish(5L)).thenReturn(
//                new EventReadDto(
//                        5L, 1L, "Main Hall", "Show",
//                        LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(2),
//                        "PUBLISHED", new BigDecimal("49.99")));
//        mvc.perform(patch("/api/admin/events/5/publish")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("PUBLISHED"));
//    }
//
//    @Test
//    @DisplayName("PATCH /api/admin/events/{id}/cancel -> 200")
//    void cancel_ok() throws Exception {
//        Mockito.when(eventService.cancel(eq(8L),any()))
//                .thenReturn(new EventReadDto(
//                        8L, 1L, "Main Hall", "Show",
//                        LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(2),
//                        "CANCELLED", new BigDecimal("49.99")));
//        mvc.perform(patch("/api/admin/events/8/cancel")
//                .with(user("admin").roles("ADMIN")).with(csrf())
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("{\"reason\":\"artist sick\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("CANCELLED"));
//    }
}
