package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.PageableUserTimeSlotsResponse;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private TimeSlotService timeSlotServiceMock;
    
    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private CalendarService calendarService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldGetUserTimeSlotsPageable_WhenUserExists() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-02T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-03T10:00:00Z"))
        );

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), any(), any(), any(), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, null, null, null, 0, 10);

        // Then
        assertNotNull(result);
        assertNotNull(result.user());
        assertEquals(user.getName(), result.user().name());
        assertEquals(user.getEmail(), result.user().email());
        assertNotNull(result.timeSlots());
        assertEquals(3, result.timeSlots().size()); // Should have 3 dates
        
        // Check pagination info
        assertNotNull(result.pageInfo());
        assertEquals(0, result.pageInfo().page());
        assertEquals(10, result.pageInfo().size());
        assertEquals(1, result.pageInfo().totalPages());
        assertEquals(3, result.pageInfo().totalElements());
        assertFalse(result.pageInfo().hasNext());
        assertFalse(result.pageInfo().hasPrevious());

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock).fetchFilteredTimeSlots(eq(userId), any(), any(), any(), any());
    }

    @Test
    void shouldGetUserTimeSlotsPageable_WithPagination() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-02T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-03T10:00:00Z"))
        );

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), any(), any(), any(), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, null, null, null, 0, 2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.timeSlots().size()); // Should have 2 dates (page size)
        
        // Check pagination info
        assertEquals(0, result.pageInfo().page());
        assertEquals(2, result.pageInfo().size());
        assertEquals(2, result.pageInfo().totalPages()); // 3 dates / 2 per page = 2 pages
        assertEquals(3, result.pageInfo().totalElements());
        assertTrue(result.pageInfo().hasNext());
        assertFalse(result.pageInfo().hasPrevious());
    }

    @Test
    void shouldGetUserTimeSlotsPageable_SecondPage() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-02T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-03T10:00:00Z"))
        );

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), any(), any(), any(), any()))
                .thenReturn(timeSlots);

        // When - Request second page
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, null, null, null, 1, 2);

        // Then
        assertNotNull(result);
        assertEquals(1, result.timeSlots().size()); // Should have 1 date (remaining)
        
        // Check pagination info
        assertEquals(1, result.pageInfo().page());
        assertEquals(2, result.pageInfo().size());
        assertEquals(2, result.pageInfo().totalPages());
        assertEquals(3, result.pageInfo().totalElements());
        assertFalse(result.pageInfo().hasNext());
        assertTrue(result.pageInfo().hasPrevious());
    }

    @Test
    void shouldGetUserTimeSlotsPageable_WithStatusFilter() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")));

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), any(), any(), eq(TimeSlot.SlotStatus.AVAILABLE), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, null, null, TimeSlot.SlotStatus.AVAILABLE, 0, 10);

        // Then
        assertNotNull(result);
        assertNotNull(result.timeSlots());
        assertEquals(1, result.timeSlots().size());

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock).fetchFilteredTimeSlots(eq(userId), any(), any(), eq(TimeSlot.SlotStatus.AVAILABLE), any());
    }

    @Test
    void shouldGetUserTimeSlotsPageable_WithDateRangeFilter() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")));
        var startDate = LocalDate.of(2026, 2, 1);
        var endDate = LocalDate.of(2026, 2, 28);

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), eq(startDate), eq(endDate), any(), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, startDate, endDate, null, 0, 10);

        // Then
        assertNotNull(result);
        assertNotNull(result.timeSlots());

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock).fetchFilteredTimeSlots(eq(userId), eq(startDate), eq(endDate), any(), any());
    }

    @Test
    void shouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // Given
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, 
                () -> calendarService.getUserTimeSlotsPageable(userId, null, null, null, 0, 10));

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock, never()).fetchFilteredTimeSlots(any(), any(), any(), any(), any());
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

    private TimeSlot createTestTimeSlotWithDate(User user, Instant startTime) {
        return TimeSlot.builder()
                .id(UUID.randomUUID())
                .user(user)
                .startTime(startTime)
                .endTime(startTime.plusSeconds(3600)) // 1 hour duration
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
