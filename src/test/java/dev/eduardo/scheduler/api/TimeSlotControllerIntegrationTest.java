package dev.eduardo.scheduler.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eduardo.scheduler.TestcontainersConfiguration;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.TimeSlotRepository;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ObjectMapper.class })
class TimeSlotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private User testUser;
    private final Instant startTime = Instant.parse("2026-02-01T10:00:00Z");
    private final Instant endTime = Instant.parse("2026-02-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        // Clean up existing data
        timeSlotRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .name("Integration Test User")
                .email("integration@example.com")
                .timezone("UTC")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldGetUserTimeSlots() throws Exception {
        // Given - Create time slot first using admin endpoint
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlotRepository.save(timeSlot);

        // When & Then - Test calendar endpoint
        mockMvc.perform(get("/api/time-slots/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value(testUser.getName()))
                .andExpect(jsonPath("$.user.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots[0].date").value("2026-02-01"))
                .andExpect(jsonPath("$.timeSlots[0].slots").isArray())
                .andExpect(jsonPath("$.timeSlots[0].slots[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.timeSlots[0].slots[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.timeSlots[0].slots[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.pageInfo.page").value(0))
                .andExpect(jsonPath("$.pageInfo.size").value(10))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrevious").value(false));
    }

    @Test
    void shouldGetUserTimeSlotsWithPagination() throws Exception {
        // Given - Create multiple time slots on different dates
        var timeSlot1 = TimeSlot.builder()
                .user(testUser)
                .startTime(Instant.parse("2026-02-01T10:00:00Z"))
                .endTime(Instant.parse("2026-02-01T11:00:00Z"))
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        
        var timeSlot2 = TimeSlot.builder()
                .user(testUser)
                .startTime(Instant.parse("2026-02-02T14:00:00Z"))
                .endTime(Instant.parse("2026-02-02T15:00:00Z"))
                .status(TimeSlot.SlotStatus.BUSY)
                .build();
        
        var timeSlot3 = TimeSlot.builder()
                .user(testUser)
                .startTime(Instant.parse("2026-02-03T16:00:00Z"))
                .endTime(Instant.parse("2026-02-03T17:00:00Z"))
                .status(TimeSlot.SlotStatus.BOOKED)
                .build();

        timeSlotRepository.saveAll(List.of(timeSlot1, timeSlot2, timeSlot3));

        // When & Then - Test pagination (page=0, size=2)
        mockMvc.perform(get("/api/time-slots/user/" + testUser.getId() + "?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value(testUser.getName()))
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots.length()").value(2)) // Should have 2 dates
                .andExpect(jsonPath("$.pageInfo.page").value(0))
                .andExpect(jsonPath("$.pageInfo.size").value(2))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(2))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(3))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
                .andExpect(jsonPath("$.pageInfo.hasPrevious").value(false));

        // Test second page
        mockMvc.perform(get("/api/time-slots/user/" + testUser.getId() + "?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlots.length()").value(1)) // Should have 1 date
                .andExpect(jsonPath("$.pageInfo.page").value(1))
                .andExpect(jsonPath("$.pageInfo.hasNext").value(false))
                .andExpect(jsonPath("$.pageInfo.hasPrevious").value(true));
    }

    @Test
    void shouldGetUserTimeSlotsWithFilters() throws Exception {
        // Given - Create time slots with different statuses
        var availableSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(Instant.parse("2026-02-01T10:00:00Z"))
                .endTime(Instant.parse("2026-02-01T11:00:00Z"))
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        
        var busySlot = TimeSlot.builder()
                .user(testUser)
                .startTime(Instant.parse("2026-02-02T14:00:00Z"))
                .endTime(Instant.parse("2026-02-02T15:00:00Z"))
                .status(TimeSlot.SlotStatus.BUSY)
                .build();

        timeSlotRepository.saveAll(List.of(availableSlot, busySlot));

        // When & Then - Test status filter
        mockMvc.perform(get("/api/time-slots/user/" + testUser.getId() + "?status=AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots.length()").value(1))
                .andExpect(jsonPath("$.timeSlots[0].slots[0].status").value("AVAILABLE"));

        // Test date range filter
        mockMvc.perform(get("/api/time-slots/user/" + testUser.getId() + "?startDate=2026-02-01&endDate=2026-02-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots.length()").value(1))
                .andExpect(jsonPath("$.timeSlots[0].date").value("2026-02-01"));
    }

    @Test
    void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/time-slots/user/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
