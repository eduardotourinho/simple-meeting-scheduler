package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.api.dto.TimeSlotResponse;
import dev.eduardo.scheduler.config.SecurityConfig;
import dev.eduardo.scheduler.service.AuthService;
import dev.eduardo.scheduler.service.TimeSlotAdminApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeSlotAdminController.class)
@Import({SecurityConfig.class, AuthService.class})
class TimeSlotAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotAdminApiService timeSlotAdminService;

    @Test
    void shouldReturn401WhenNoAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/admin/time-slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Authorization token is required\"}"));
    }

    @Test
    void shouldReturn401WhenInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/admin/time-slots")
                .header("Authorization", "InvalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Authorization token is required\"}"));
    }

    @Test
    void shouldReturn401WhenEmptyBearerToken() throws Exception {
        mockMvc.perform(post("/api/admin/time-slots")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Invalid token\"}"));
    }

    @Test
    void shouldReturn401WhenInvalidUuidToken() throws Exception {
        mockMvc.perform(post("/api/admin/time-slots")
                .header("Authorization", "Bearer invalid-uuid-format")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Invalid token format\"}"));
    }

    @Test
    void shouldAllowAccessWithValidUuidToken() throws Exception {
        String validUserUuid = "123e4567-e89b-12d3-a456-426614174000";
        UUID timeSlotId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        UUID userId = UUID.fromString(validUserUuid);
        
        // Mock the service response
        when(timeSlotAdminService.getTimeSlot(timeSlotId, userId))
                .thenReturn(TimeSlotResponse.builder()
                        .id(timeSlotId)
                        .userId(userId)
                        .userEmail("test@example.com")
                        .startTime(java.time.Instant.now())
                        .endTime(java.time.Instant.now().plusSeconds(3600))
                        .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                        .build());
        
        mockMvc.perform(get("/api/admin/time-slots/123e4567-e89b-12d3-a456-426614174001")
                .header("Authorization", "Bearer " + validUserUuid))
                .andExpect(status().isOk());
    }
}
