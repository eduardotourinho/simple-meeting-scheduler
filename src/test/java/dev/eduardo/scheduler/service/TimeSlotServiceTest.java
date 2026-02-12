package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.TimeSlotRepository;
import dev.eduardo.scheduler.service.exception.TimeSlotNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepositoryMock;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private final UUID userId = UUID.randomUUID();
    private final UUID timeSlotId = UUID.randomUUID();
    private final Instant startTime = Instant.parse("2024-01-01T10:00:00Z");
    private final Instant endTime = Instant.parse("2024-01-01T11:00:00Z");

    @Test
    void shouldCreateSlot_WhenValidInput() {
        // Given
        var user = createTestUser();
        var timeSlotToCreate = createTestTimeSlot(user);
        var expectedTimeSlot = createTestTimeSlot(user);

        when(timeSlotRepositoryMock.save(timeSlotToCreate)).thenReturn(expectedTimeSlot);

        // When
        TimeSlot result = timeSlotService.createSlot(timeSlotToCreate);

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlot.getId(), result.getId());
        assertEquals(user, result.getUser());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(TimeSlot.SlotStatus.AVAILABLE, result.getStatus());

        verify(timeSlotRepositoryMock).save(timeSlotToCreate);
    }

    @Test
    void shouldUpdateSlot_WhenValidInput() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);

        when(timeSlotRepositoryMock.save(timeSlot)).thenReturn(timeSlot);

        // When
        TimeSlot result = timeSlotService.updateSlot(timeSlot);

        // Then
        assertNotNull(result);
        assertEquals(timeSlot, result);

        verify(timeSlotRepositoryMock).save(timeSlot);
    }

    @Test
    void shouldRemoveSlot_WhenValidInput() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);

        // When
        timeSlotService.removeSlot(timeSlot);

        // Then
        verify(timeSlotRepositoryMock).delete(timeSlot);
    }

    @Test
    void shouldFindById_WhenTimeSlotExists() {
        // Given
        var user = createTestUser();
        var expectedTimeSlot = createTestTimeSlot(user);

        when(timeSlotRepositoryMock.findById(timeSlotId)).thenReturn(Optional.of(expectedTimeSlot));

        // When
        TimeSlot result = timeSlotService.findById(timeSlotId);

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlot, result);

        verify(timeSlotRepositoryMock).findById(timeSlotId);
    }

    @Test
    void shouldThrowTimeSlotNotFoundException_WhenTimeSlotDoesNotExist() {
        // Given
        when(timeSlotRepositoryMock.findById(timeSlotId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(TimeSlotNotFoundException.class, 
                () -> timeSlotService.findById(timeSlotId));

        verify(timeSlotRepositoryMock).findById(timeSlotId);
    }

    @Test
    void shouldFetchFilteredTimeSlots_WithoutFilters() {
        // Given
        var user = createTestUser();
        var expectedTimeSlots = List.of(createTestTimeSlot(user));

        when(timeSlotRepositoryMock.findByUserIdOrderByStartTime(userId)).thenReturn(expectedTimeSlots);

        // When
        List<TimeSlot> result = timeSlotService.fetchFilteredTimeSlots(userId, null, null, null, ZoneId.of("UTC"));

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlots, result);

        verify(timeSlotRepositoryMock).findByUserIdOrderByStartTime(userId);
    }

    @Test
    void shouldFetchFilteredTimeSlots_WithStatusFilter() {
        // Given
        var user = createTestUser();
        var expectedTimeSlots = List.of(createTestTimeSlot(user));

        when(timeSlotRepositoryMock.findByUserIdAndStatusOrderByStartTime(userId, TimeSlot.SlotStatus.AVAILABLE))
                .thenReturn(expectedTimeSlots);

        // When
        List<TimeSlot> result = timeSlotService.fetchFilteredTimeSlots(userId, null, null, TimeSlot.SlotStatus.AVAILABLE, ZoneId.of("UTC"));

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlots, result);

        verify(timeSlotRepositoryMock).findByUserIdAndStatusOrderByStartTime(userId, TimeSlot.SlotStatus.AVAILABLE);
    }

    @Test
    void shouldFetchFilteredTimeSlots_WithDateRangeFilter() {
        // Given
        var user = createTestUser();
        var expectedTimeSlots = List.of(createTestTimeSlot(user));
        var startDate = LocalDate.of(2024, 1, 1);
        var endDate = LocalDate.of(2024, 1, 31);
        var zoneId = ZoneId.of("UTC");
        var startInstant = startDate.atStartOfDay(zoneId).toInstant();
        var endInstant = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        when(timeSlotRepositoryMock.findByUserIdAndTimeRange(userId, startInstant, endInstant))
                .thenReturn(expectedTimeSlots);

        // When
        List<TimeSlot> result = timeSlotService.fetchFilteredTimeSlots(userId, startDate, endDate, null, zoneId);

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlots, result);

        verify(timeSlotRepositoryMock).findByUserIdAndTimeRange(userId, startInstant, endInstant);
    }

    @Test
    void shouldFetchFilteredTimeSlots_WithAllFilters() {
        // Given
        var user = createTestUser();
        var expectedTimeSlots = List.of(createTestTimeSlot(user));
        var startDate = LocalDate.of(2024, 1, 1);
        var endDate = LocalDate.of(2024, 1, 31);
        var zoneId = ZoneId.of("UTC");
        var startInstant = startDate.atStartOfDay(zoneId).toInstant();
        var endInstant = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        when(timeSlotRepositoryMock.findByUserIdAndStatusAndTimeRange(userId, TimeSlot.SlotStatus.AVAILABLE, startInstant, endInstant))
                .thenReturn(expectedTimeSlots);

        // When
        List<TimeSlot> result = timeSlotService.fetchFilteredTimeSlots(userId, startDate, endDate, TimeSlot.SlotStatus.AVAILABLE, zoneId);

        // Then
        assertNotNull(result);
        assertEquals(expectedTimeSlots, result);

        verify(timeSlotRepositoryMock).findByUserIdAndStatusAndTimeRange(userId, TimeSlot.SlotStatus.AVAILABLE, startInstant, endInstant);
    }

    @Test
    void shouldHasOverlappingSlots_WithoutExclusion() {
        // Given
        createTestUser();

        when(timeSlotRepositoryMock.existsOverlappingSlot(userId, startTime, endTime)).thenReturn(true);

        // When
        boolean result = timeSlotService.hasOverlappingSlots(userId, startTime, endTime);

        // Then
        assertTrue(result);

        verify(timeSlotRepositoryMock).existsOverlappingSlot(userId, startTime, endTime);
    }

    @Test
    void shouldHasOverlappingSlots_WithExclusion() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);

        when(timeSlotRepositoryMock.existsOverlappingSlot(userId, startTime, endTime, timeSlotId)).thenReturn(false);

        // When
        boolean result = timeSlotService.hasOverlappingSlots(userId, startTime, endTime, timeSlot);

        // Then
        assertFalse(result);

        verify(timeSlotRepositoryMock).existsOverlappingSlot(userId, startTime, endTime, timeSlotId);
    }

    private User createTestUser() {
        return User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private TimeSlot createTestTimeSlot(User user) {
        return TimeSlot.builder()
                .id(timeSlotId)
                .user(user)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
