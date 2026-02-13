package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.CreateTimeSlotRequest;
import dev.eduardo.scheduler.api.dto.TimeSlotResponse;
import dev.eduardo.scheduler.api.dto.UpdateTimeSlotRequest;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.TimeSlotNotFoundException;
import dev.eduardo.scheduler.service.exception.TimeSlotOverlapException;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotAdminApiServiceTest {

    @Mock
    private TimeSlotService timeSlotServiceMock;
    
    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private TimeSlotAdminApiService adminService;

    private final UUID userId = UUID.randomUUID();
    private final UUID timeSlotId = UUID.randomUUID();
    private final Instant startTime = Instant.parse("2024-01-01T10:00:00Z");
    private final Instant endTime = Instant.parse("2024-01-01T11:00:00Z");


    @Test
    void shouldCreateTimeSlots_WhenValidRequest() {
        // Given
        var user = createTestUser();
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        var timeSlot = createTestTimeSlot(user);

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.hasOverlappingSlots(userId, startTime, endTime)).thenReturn(false);
        when(timeSlotServiceMock.createSlot(any(TimeSlot.class)))
                .thenReturn(timeSlot);

        // When
        var result = adminService.createTimeSlots(request, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.createdCount());
        assertEquals(1, result.createdSlots().size());
        
        var createdSlot = result.createdSlots().getFirst();
        assertEquals(timeSlotId, createdSlot.id());
        assertEquals(userId, createdSlot.userId());
        assertEquals(startTime, createdSlot.startTime());
        assertEquals(endTime, createdSlot.endTime());
        assertEquals(TimeSlot.SlotStatus.AVAILABLE, createdSlot.status());

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock).hasOverlappingSlots(userId, startTime, endTime);
        verify(timeSlotServiceMock).createSlot(any(TimeSlot.class));
    }

    @Test
    void shouldThrowUserNotFoundException_WhenUserDoesNotExist() {
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

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, 
                () -> adminService.createTimeSlots(request, userId));

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock, never()).hasOverlappingSlots(any(), any(), any());
        verify(timeSlotServiceMock, never()).createSlot(any(TimeSlot.class));
    }

    @Test
    void shouldThrowTimeSlotOverlapException_WhenTimeSlotOverlaps() {
        // Given
        var user = createTestUser();
        var request = CreateTimeSlotRequest.builder()
                .slots(List.of(
                        CreateTimeSlotRequest.TimeSlotData.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .status(TimeSlot.SlotStatus.AVAILABLE)
                                .build()
                ))
                .build();

        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));
        when(timeSlotServiceMock.hasOverlappingSlots(userId, startTime, endTime)).thenReturn(true);

        // When & Then
        assertThrows(TimeSlotOverlapException.class, 
                () -> adminService.createTimeSlots(request, userId));

        verify(userRepositoryMock).findById(userId);
        verify(timeSlotServiceMock).hasOverlappingSlots(userId, startTime, endTime);
        verify(timeSlotServiceMock, never()).createSlot(any(TimeSlot.class));
    }

    @Test
    void shouldGetTimeSlot_WhenExists() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When
        TimeSlotResponse result = adminService.getTimeSlot(timeSlotId, userId);

        // Then
        assertNotNull(result);
        assertEquals(timeSlotId, result.id());
        assertEquals(userId, result.userId());

        verify(timeSlotServiceMock).findById(timeSlotId);
    }

    @Test
    void shouldThrowTimeSlotNotFoundException_WhenTimeSlotDoesNotExist() {
        // Given
        when(timeSlotServiceMock.findById(timeSlotId))
                .thenThrow(new TimeSlotNotFoundException("Time slot not found with ID: " + timeSlotId));

        // When & Then
        assertThrows(TimeSlotNotFoundException.class, 
                () -> adminService.getTimeSlot(timeSlotId, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
    }

    @Test
    void shouldUpdateTimeSlot_WhenValidRequest() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);
        var newEndTime = Instant.parse("2024-01-01T12:00:00Z");
        
        var request = UpdateTimeSlotRequest.builder()
                .startTime(startTime)
                .endTime(newEndTime)
                .status(TimeSlot.SlotStatus.BUSY)
                .build();

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(timeSlotServiceMock.hasOverlappingSlots(userId, startTime, newEndTime, timeSlot))
                .thenReturn(false);
        when(timeSlotServiceMock.updateSlot(timeSlot)).thenReturn(timeSlot);

        // When
        TimeSlotResponse result = adminService.updateTimeSlot(timeSlotId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals(timeSlotId, result.id());

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock).hasOverlappingSlots(userId, startTime, newEndTime, timeSlot);
        verify(timeSlotServiceMock).updateSlot(timeSlot);
    }

    @Test
    void shouldThrowTimeSlotOverlapException_WhenUpdateCausesOverlap() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);
        var newEndTime = Instant.parse("2024-01-01T12:00:00Z");
        
        var request = UpdateTimeSlotRequest.builder()
                .startTime(startTime)
                .endTime(newEndTime)
                .status(TimeSlot.SlotStatus.BUSY)
                .build();

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(timeSlotServiceMock.hasOverlappingSlots(userId, startTime, newEndTime, timeSlot))
                .thenReturn(true);

        // When & Then
        assertThrows(TimeSlotOverlapException.class, 
                () -> adminService.updateTimeSlot(timeSlotId, request, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock).hasOverlappingSlots(userId, startTime, newEndTime, timeSlot);
        verify(timeSlotServiceMock, never()).updateSlot(any());
    }

    @Test
    void shouldDeleteTimeSlot_WhenExists() {
        // Given
        var user = createTestUser();
        var timeSlot = createTestTimeSlot(user);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When
        adminService.deleteTimeSlot(timeSlotId, userId);

        // Then
        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock).removeSlot(timeSlot);
    }

    @Test
    void shouldThrowTimeSlotNotFoundException_WhenDeletingNonExistentTimeSlot() {
        // Given
        when(timeSlotServiceMock.findById(timeSlotId))
                .thenThrow(new TimeSlotNotFoundException("Time slot not found with ID: " + timeSlotId));

        // When & Then
        assertThrows(TimeSlotNotFoundException.class, 
                () -> adminService.deleteTimeSlot(timeSlotId, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock, never()).removeSlot(any());
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenDeletingBookedTimeSlot() {
        // Given
        var user = createTestUser();
        var timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .user(user)
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.BOOKED) // Booked status
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> adminService.deleteTimeSlot(timeSlotId, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock, never()).removeSlot(any());
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenGettingTimeSlotOfDifferentUser() {
        // Given
        var differentUserId = UUID.randomUUID();
        var differentUser = User.builder()
                .id(differentUserId)
                .name("Different User")
                .email("different@example.com")
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var timeSlot = createTestTimeSlot(differentUser);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> adminService.getTimeSlot(timeSlotId, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenUpdatingTimeSlotOfDifferentUser() {
        // Given
        var differentUserId = UUID.randomUUID();
        var differentUser = User.builder()
                .id(differentUserId)
                .name("Different User")
                .email("different@example.com")
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var timeSlot = createTestTimeSlot(differentUser);
        
        var request = UpdateTimeSlotRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .status(TimeSlot.SlotStatus.BUSY)
                .build();

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> adminService.updateTimeSlot(timeSlotId, request, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock, never()).hasOverlappingSlots(any(), any(), any(), any());
        verify(timeSlotServiceMock, never()).updateSlot(any());
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenDeletingTimeSlotOfDifferentUser() {
        // Given
        var differentUserId = UUID.randomUUID();
        var differentUser = User.builder()
                .id(differentUserId)
                .name("Different User")
                .email("different@example.com")
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var timeSlot = createTestTimeSlot(differentUser);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> adminService.deleteTimeSlot(timeSlotId, userId));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(timeSlotServiceMock, never()).removeSlot(any());
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
