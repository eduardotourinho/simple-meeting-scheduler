package dev.eduardo.scheduler.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eduardo.scheduler.TestcontainersConfiguration;
import dev.eduardo.scheduler.api.dto.BulkCreateTimeSlotsResponse;
import dev.eduardo.scheduler.api.dto.CreateTimeSlotRequest;
import dev.eduardo.scheduler.api.dto.UpdateTimeSlotRequest;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import( { TestcontainersConfiguration.class, ObjectMapper.class })
class TimeSlotAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private final Instant startTime = Instant.parse("2026-02-01T10:00:00Z");
    private final Instant endTime = Instant.parse("2026-02-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        // Use a unique email for each test run to avoid conflicts
        String uniqueEmail = "admin-test-" + System.currentTimeMillis() + "@test.com";
        
        testUser = User.builder()
                .name("Admin Test User")
                .email(uniqueEmail)
                .timezone("UTC")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCreateTimeSlots_WhenValidRequest() throws Exception {
        // Given
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.createdSlots").isArray())
                .andExpect(jsonPath("$.createdSlots[0].userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.createdSlots[0].userEmail").value(testUser.getEmail()))
                .andExpect(jsonPath("$.createdSlots[0].startTime").value(startTime.toString()))
                .andExpect(jsonPath("$.createdSlots[0].endTime").value(endTime.toString()))
                .andExpect(jsonPath("$.createdSlots[0].status").value("AVAILABLE"));
    }

    @Test
    void shouldReturnBadRequest_WhenEndTimeBeforeStartTime() throws Exception {
        // Given
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(endTime)
                                .endTime(startTime) // Invalid: end before start
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("End time must be after start time for slot starting at " + endTime));
    }

    @Test
    void shouldCreateMultipleTimeSlots_WhenValidRequest() throws Exception {
        // Given
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(TimeSlot.SlotStatus.AVAILABLE)
                                .build(),
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(Instant.parse("2026-02-01T14:00:00Z"))
                                .endTime(Instant.parse("2026-02-01T15:00:00Z"))
                                .status(TimeSlot.SlotStatus.BUSY)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.createdSlots").isArray())
                .andExpect(jsonPath("$.createdSlots.length()").value(2))
                .andExpect(jsonPath("$.createdSlots[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdSlots[1].status").value("BUSY"));
    }

    @Test
    void shouldReturnConflict_WhenTimeSlotOverlaps() throws Exception {
        // Given - Create first time slot
        var firstRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // When - Try to create overlapping time slot
        var overlappingRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime.minusSeconds(1800)) // 30 minutes before
                                .endTime(endTime.plusSeconds(1800)) // 30 minutes after
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.BUSY)
                                .build()
                ))
                .build();

        // Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // Given
        var nonExistentUserId = UUID.randomUUID();
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + nonExistentUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldGetTimeSlot_WhenExists() throws Exception {
        // Given - Create a time slot first
        var createRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        var createResult = mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdTimeSlot = objectMapper.readValue(createResult.getResponse().getContentAsString(), BulkCreateTimeSlotsResponse.class);
        assertEquals(1, createdTimeSlot.createdSlots().size());
        var timeSlotId = createdTimeSlot.createdSlots().getFirst().id();

        // When & Then
        mockMvc.perform(get("/api/admin/time-slots/" + timeSlotId)
                        .header("Authorization", "Bearer " + testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timeSlotId.toString()))
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.startTime").value(startTime.toString()))
                .andExpect(jsonPath("$.endTime").value(endTime.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void shouldReturnNotFound_WhenGettingNonExistentTimeSlot() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/time-slots/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + testUser.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldUpdateTimeSlot_WhenValidRequest() throws Exception {
        // Given - Create a time slot first
        var createRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        var createResult = mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdTimeSlot = objectMapper.readValue(createResult.getResponse().getContentAsString(), BulkCreateTimeSlotsResponse.class);
        assertEquals(1, createdTimeSlot.createdSlots().size());
        var timeSlotId = createdTimeSlot.createdSlots().getFirst().id();

        var newEndTime = Instant.parse("2026-02-01T12:00:00Z");
        var updateRequest = UpdateTimeSlotRequest.builder()
                .startTime(startTime)
                .endTime(newEndTime)
                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.BUSY)
                .build();

        // When & Then
        mockMvc.perform(put("/api/admin/time-slots/" + timeSlotId)
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timeSlotId.toString()))
                .andExpect(jsonPath("$.startTime").value(startTime.toString()))
                .andExpect(jsonPath("$.endTime").value(newEndTime.toString()))
                .andExpect(jsonPath("$.status").value("BUSY"));
    }

    @Test
    void shouldDeleteTimeSlot_WhenExists() throws Exception {
        // Given - Create a time slot first
        var createRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        var createResult = mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdTimeSlot = objectMapper.readValue(createResult.getResponse().getContentAsString(), BulkCreateTimeSlotsResponse.class);
        assertEquals(1, createdTimeSlot.createdSlots().size());
        var timeSlotId = createdTimeSlot.createdSlots().getFirst().id();

        // When & Then
        mockMvc.perform(delete("/api/admin/time-slots/" + timeSlotId)
                        .header("Authorization", "Bearer " + testUser.getId().toString()))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/admin/time-slots/" + timeSlotId)
                        .header("Authorization", "Bearer " + testUser.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequest_WhenDeletingBookedTimeSlot() throws Exception {
        // Given - Create a booked time slot
        var createRequest = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.BOOKED)
                                .build()
                ))
                .build();

        var createResult = mockMvc.perform(post("/api/admin/time-slots")
                        .header("Authorization", "Bearer " + testUser.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdTimeSlot = objectMapper.readValue(createResult.getResponse().getContentAsString(), BulkCreateTimeSlotsResponse.class);
        assertEquals(1, createdTimeSlot.createdSlots().size());
        var timeSlotId = createdTimeSlot.createdSlots().getFirst().id();

        // When & Then
        mockMvc.perform(delete("/api/admin/time-slots/" + timeSlotId)
                        .header("Authorization", "Bearer " + testUser.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturnUnauthorized_WhenMissingAuthorizationHeader() throws Exception {
        // Given
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(dev.eduardo.scheduler.domain.entities.TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authorization token is required"));
    }
}
