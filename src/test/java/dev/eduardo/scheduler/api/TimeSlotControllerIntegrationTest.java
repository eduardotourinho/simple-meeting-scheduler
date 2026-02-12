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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // Meeting Creation Integration Tests

    @Test
    void shouldCreateMeeting_WithValidRequest() throws Exception {
        // Given - Create available time slot
        var timeSlot = timeSlotRepository.save(TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build());

        // Create user for internal participant
        userRepository.save(User.builder()
                .name("John Doe")
                .email("john@example.com")
                .timezone("UTC")
                .build());

        var requestBody = """
                {
                    "title": "Integration Test Meeting",
                    "description": "Meeting created via integration test",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        },
                        {
                            "name": "Jane Smith",
                            "email": "jane@example.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").exists())
                .andExpect(jsonPath("$.timeSlotId").value(timeSlot.getId().toString()))
                .andExpect(jsonPath("$.title").value("Integration Test Meeting"))
                .andExpect(jsonPath("$.description").value("Meeting created via integration test"))
                .andExpect(jsonPath("$.organizerId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.organizerEmail").value(testUser.getEmail()))
                .andExpect(jsonPath("$.startTime").value(startTime.toString()))
                .andExpect(jsonPath("$.endTime").value(endTime.toString()))
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[?(@.name == 'John Doe')].email").value("john@example.com"))
                .andExpect(jsonPath("$.participants[?(@.name == 'John Doe')].type").value("INTERNAL"))
                .andExpect(jsonPath("$.participants[?(@.name == 'John Doe')].status").value("INVITED"))
                .andExpect(jsonPath("$.participants[?(@.name == 'Jane Smith')].email").value("jane@example.com"))
                .andExpect(jsonPath("$.participants[?(@.name == 'Jane Smith')].type").value("EXTERNAL"))
                .andExpect(jsonPath("$.participants[?(@.name == 'Jane Smith')].status").value("INVITED"));

        // Verify time slot was updated to BOOKED
        var updatedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertEquals(TimeSlot.SlotStatus.BOOKED, updatedTimeSlot.getStatus());
    }

    @Test
    void shouldCreateMeeting_WithExternalParticipantsOnly() throws Exception {
        // Given - Create available time slot
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlot = timeSlotRepository.save(timeSlot);

        var requestBody = """
                {
                    "title": "External Only Meeting",
                    "participants": [
                        {
                            "name": "Alice Brown",
                            "email": "alice@external.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("External Only Meeting"))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].name").value("Alice Brown"))
                .andExpect(jsonPath("$.participants[0].email").value("alice@external.com"))
                .andExpect(jsonPath("$.participants[0].type").value("EXTERNAL"));
    }

    @Test
    void shouldCreateMeeting_WithoutDescription() throws Exception {
        // Given
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlot = timeSlotRepository.save(timeSlot);

        var requestBody = """
                {
                    "title": "Meeting without description",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Meeting without description"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void shouldReturnBadRequest_WhenTitleIsMissing() throws Exception {
        // Given
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlot = timeSlotRepository.save(timeSlot);

        var requestBody = """
                {
                    "description": "Missing title",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_WhenParticipantsAreEmpty() throws Exception {
        // Given
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlot = timeSlotRepository.save(timeSlot);

        var requestBody = """
                {
                    "title": "Meeting without participants",
                    "participants": []
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_WhenParticipantEmailIsInvalid() throws Exception {
        // Given
        var timeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .build();
        timeSlot = timeSlotRepository.save(timeSlot);

        var requestBody = """
                {
                    "title": "Meeting with invalid email",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "invalid-email"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + timeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_WhenTimeSlotDoesNotExist() throws Exception {
        // Given
        var nonExistentTimeSlotId = UUID.randomUUID();

        var requestBody = """
                {
                    "title": "Meeting for non-existent slot",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + nonExistentTimeSlotId + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflict_WhenTimeSlotIsNotAvailable() throws Exception {
        // Given - Create busy time slot
        var busyTimeSlot = TimeSlot.builder()
                .user(testUser)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.BOOKED)
                .build();
        busyTimeSlot = timeSlotRepository.save(busyTimeSlot);

        var requestBody = """
                {
                    "title": "Meeting on busy slot",
                    "participants": [
                        {
                            "name": "John Doe",
                            "email": "john@example.com"
                        }
                    ]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/time-slots/" + busyTimeSlot.getId() + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }
}
