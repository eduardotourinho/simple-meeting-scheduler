package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.CreateMeetingRequest;
import dev.eduardo.scheduler.api.dto.CreateMeetingResponse;
import dev.eduardo.scheduler.api.dto.PageableUserTimeSlotsResponse;
import dev.eduardo.scheduler.domain.entities.Meeting;
import dev.eduardo.scheduler.domain.entities.MeetingParticipant;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.service.exception.TimeSlotNotAvailableException;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private UserService userServiceMock;

    @Mock
    private MeetingService meetingServiceMock;

    @InjectMocks
    private CalendarService calendarService;

    private final UUID userId = UUID.randomUUID();
    private final UUID timeSlotId = UUID.randomUUID();

    @Test
    void shouldGetUserTimeSlotsPageable_WhenUserExists() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-02T10:00:00Z")),
                createTestTimeSlotWithDate(user, Instant.parse("2026-02-03T10:00:00Z"))
        );

        when(userServiceMock.findById(userId)).thenReturn(user);
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

        verify(userServiceMock).findById(userId);
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

        when(userServiceMock.findById(userId)).thenReturn(user);
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

        when(userServiceMock.findById(userId)).thenReturn(user);
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

        when(userServiceMock.findById(userId)).thenReturn(user);
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), any(), any(), eq(TimeSlot.SlotStatus.AVAILABLE), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, null, null, TimeSlot.SlotStatus.AVAILABLE, 0, 10);

        // Then
        assertNotNull(result);
        assertNotNull(result.timeSlots());
        assertEquals(1, result.timeSlots().size());

        verify(userServiceMock).findById(userId);
        verify(timeSlotServiceMock).fetchFilteredTimeSlots(eq(userId), any(), any(), eq(TimeSlot.SlotStatus.AVAILABLE), any());
    }

    @Test
    void shouldGetUserTimeSlotsPageable_WithDateRangeFilter() {
        // Given
        var user = createTestUser();
        var timeSlots = List.of(createTestTimeSlotWithDate(user, Instant.parse("2026-02-01T10:00:00Z")));
        var startDate = LocalDate.of(2026, 2, 1);
        var endDate = LocalDate.of(2026, 2, 28);

        when(userServiceMock.findById(userId)).thenReturn(user);
        when(timeSlotServiceMock.fetchFilteredTimeSlots(eq(userId), eq(startDate), eq(endDate), any(), any()))
                .thenReturn(timeSlots);

        // When
        PageableUserTimeSlotsResponse result = calendarService.getUserTimeSlotsPageable(userId, startDate, endDate, null, 0, 10);

        // Then
        assertNotNull(result);
        assertNotNull(result.timeSlots());

        verify(userServiceMock).findById(userId);
        verify(timeSlotServiceMock).fetchFilteredTimeSlots(eq(userId), eq(startDate), eq(endDate), any(), any());
    }

    @Test
    void shouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // Given
        when(userServiceMock.findById(userId)).thenThrow(new UserNotFoundException("User not found"));

        // When & Then
        assertThrows(UserNotFoundException.class, 
                () -> calendarService.getUserTimeSlotsPageable(userId, null, null, null, 0, 10));

        verify(userServiceMock).findById(userId);
        verify(timeSlotServiceMock, never()).fetchFilteredTimeSlots(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateMeeting_WhenTimeSlotIsAvailable() {
        // Given
        var timeSlot = createTestTimeSlot();
        var request = createTestMeetingRequest();
        var expectedMeeting = createTestMeeting(timeSlot, request);
        var savedMeeting = createSavedMeeting(expectedMeeting);
        var internalParticipant = createInternalParticipant(savedMeeting);
        var externalParticipant = createExternalParticipant(savedMeeting, "Jane Smith", "jane@example.com");

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(meetingServiceMock.saveMeeting(expectedMeeting)).thenReturn(savedMeeting);
        when(userServiceMock.findByEmail("john@example.com")).thenReturn(Optional.of(timeSlot.getUser()));
        when(userServiceMock.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(meetingServiceMock.createInternalParticipant(savedMeeting, timeSlot.getUser())).thenReturn(internalParticipant);
        when(meetingServiceMock.createExternalParticipant(savedMeeting, "Jane Smith", "jane@example.com")).thenReturn(externalParticipant);

        // When
        CreateMeetingResponse result = calendarService.createMeeting(timeSlotId, request);

        // Then
        assertNotNull(result);
        assertEquals(savedMeeting.getId(), result.meetingId());
        assertEquals(timeSlotId, result.timeSlotId());
        assertEquals(request.title(), result.title());
        assertEquals(request.description(), result.description());
        assertEquals(timeSlot.getUser().getId(), result.organizerId());
        assertEquals(timeSlot.getUser().getEmail(), result.organizerEmail());
        assertEquals(2, result.participants().size());

        // Verify time slot status was updated
        assertEquals(TimeSlot.SlotStatus.BOOKED, timeSlot.getStatus());
        verify(timeSlotServiceMock).updateSlot(timeSlot);

        // Verify participant creation
        verify(userServiceMock).findByEmail("john@example.com");
        verify(userServiceMock).findByEmail("jane@example.com");
        verify(meetingServiceMock).createInternalParticipant(savedMeeting, timeSlot.getUser());
        verify(meetingServiceMock).createExternalParticipant(savedMeeting, "Jane Smith", "jane@example.com");
    }

    @Test
    void shouldCreateMeeting_WithOnlyInternalParticipants() {
        // Given
        var timeSlot = createTestTimeSlot();
        var request = CreateMeetingRequest.builder()
                .title("Internal Meeting")
                .description("Internal only")
                .participants(List.of(
                        new CreateMeetingRequest.ParticipantRequest("John Doe", "john@example.com"),
                        new CreateMeetingRequest.ParticipantRequest("Jane Smith", "jane@example.com")
                ))
                .build();
        var expectedMeeting = createTestMeeting(timeSlot, request);
        var savedMeeting = createSavedMeeting(expectedMeeting);
        var participant1 = createInternalParticipant(savedMeeting);
        var participant2 = createInternalParticipant(savedMeeting);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(meetingServiceMock.saveMeeting(expectedMeeting)).thenReturn(savedMeeting);
        when(userServiceMock.findByEmail("john@example.com")).thenReturn(Optional.of(timeSlot.getUser()));
        when(userServiceMock.findByEmail("jane@example.com")).thenReturn(Optional.of(timeSlot.getUser()));
        when(meetingServiceMock.createInternalParticipant(savedMeeting, timeSlot.getUser())).thenReturn(participant1);
        when(meetingServiceMock.createInternalParticipant(savedMeeting, timeSlot.getUser())).thenReturn(participant2);

        // When
        CreateMeetingResponse result = calendarService.createMeeting(timeSlotId, request);

        // Then
        assertNotNull(result);
        assertEquals(2, result.participants().size());
        assertEquals(MeetingParticipant.ParticipantType.INTERNAL, result.participants().get(0).type());
        assertEquals(MeetingParticipant.ParticipantType.INTERNAL, result.participants().get(1).type());

        verify(meetingServiceMock, never()).createExternalParticipant(any(), any(), any());
    }

    @Test
    void shouldCreateMeeting_WithOnlyExternalParticipants() {
        // Given
        var timeSlot = createTestTimeSlot();
        var request = CreateMeetingRequest.builder()
                .title("External Meeting")
                .description("External only")
                .participants(List.of(
                        new CreateMeetingRequest.ParticipantRequest("Alice Brown", "alice@external.com")
                ))
                .build();
        var expectedMeeting = createTestMeeting(timeSlot, request);
        var savedMeeting = createSavedMeeting(expectedMeeting);
        var externalParticipant = createExternalParticipant(savedMeeting, "Alice Brown", "alice@external.com");

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(meetingServiceMock.saveMeeting(expectedMeeting)).thenReturn(savedMeeting);
        when(userServiceMock.findByEmail("alice@external.com")).thenReturn(Optional.empty());
        when(meetingServiceMock.createExternalParticipant(savedMeeting, "Alice Brown", "alice@external.com")).thenReturn(externalParticipant);

        // When
        CreateMeetingResponse result = calendarService.createMeeting(timeSlotId, request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.participants().size());
        assertEquals(MeetingParticipant.ParticipantType.EXTERNAL, result.participants().getFirst().type());
        assertEquals("alice@external.com", result.participants().getFirst().email());

        verify(meetingServiceMock, never()).createInternalParticipant(any(), any());
    }

    @Test
    void shouldThrowException_WhenTimeSlotIsNotAvailable() {
        // Given
        var busyTimeSlot = createTestTimeSlot();
        busyTimeSlot.setStatus(TimeSlot.SlotStatus.BOOKED);
        var request = createTestMeetingRequest();

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(busyTimeSlot);

        // When & Then
        assertThrows(TimeSlotNotAvailableException.class, 
                () -> calendarService.createMeeting(timeSlotId, request));

        verify(timeSlotServiceMock).findById(timeSlotId);
        verify(meetingServiceMock, never()).saveMeeting(any());
        verify(timeSlotServiceMock, never()).updateSlot(any());
    }

    @Test
    void shouldCreateMeeting_WithoutDescription() {
        // Given
        var timeSlot = createTestTimeSlot();
        var request = CreateMeetingRequest.builder()
                .title("Meeting without description")
                .description(null)
                .participants(List.of(
                        new CreateMeetingRequest.ParticipantRequest("John Doe", "john@example.com")
                ))
                .build();
        var expectedMeeting = createTestMeeting(timeSlot, request);
        var savedMeeting = createSavedMeeting(expectedMeeting);
        var participant = createInternalParticipant(savedMeeting);

        when(timeSlotServiceMock.findById(timeSlotId)).thenReturn(timeSlot);
        when(meetingServiceMock.saveMeeting(expectedMeeting)).thenReturn(savedMeeting);
        when(userServiceMock.findByEmail("john@example.com")).thenReturn(Optional.of(timeSlot.getUser()));
        when(meetingServiceMock.createInternalParticipant(savedMeeting, timeSlot.getUser())).thenReturn(participant);

        // When
        CreateMeetingResponse result = calendarService.createMeeting(timeSlotId, request);

        // Then
        assertNotNull(result);
        assertNull(result.description()); // Should be null
        assertEquals("Meeting without description", result.title());

        verify(meetingServiceMock).saveMeeting(any(Meeting.class));
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

    private TimeSlot createTestTimeSlot() {
        var user = createTestUser();
        return TimeSlot.builder()
                .id(timeSlotId)
                .user(user)
                .startTime(Instant.parse("2026-02-01T10:00:00Z"))
                .endTime(Instant.parse("2026-02-01T11:00:00Z"))
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private CreateMeetingRequest createTestMeetingRequest() {
        return CreateMeetingRequest.builder()
                .title("Test Meeting")
                .description("Test Description")
                .participants(List.of(
                        new CreateMeetingRequest.ParticipantRequest("John Doe", "john@example.com"),
                        new CreateMeetingRequest.ParticipantRequest("Jane Smith", "jane@example.com")
                ))
                .build();
    }

    private Meeting createTestMeeting(TimeSlot timeSlot, CreateMeetingRequest request) {
        return Meeting.builder()
                .timeSlot(timeSlot)
                .title(request.title())
                .description(request.description())
                .organizer(timeSlot.getUser())
                .participants(new ArrayList<>())
                .build();
    }

    private Meeting createSavedMeeting(Meeting meeting) {
        return Meeting.builder()
                .id(UUID.randomUUID())
                .timeSlot(meeting.getTimeSlot())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .organizer(meeting.getOrganizer())
                .participants(meeting.getParticipants())
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private MeetingParticipant createInternalParticipant(Meeting meeting) {
        return MeetingParticipant.builder()
                .id(UUID.randomUUID())
                .meeting(meeting)
                .user(meeting.getOrganizer())
                .participantType(MeetingParticipant.ParticipantType.INTERNAL)
                .status(MeetingParticipant.ParticipantStatus.INVITED)
                .build();
    }

    private MeetingParticipant createExternalParticipant(Meeting meeting, String name, String email) {
        return MeetingParticipant.builder()
                .id(UUID.randomUUID())
                .meeting(meeting)
                .externalEmail(email.toLowerCase())
                .externalName(name)
                .participantType(MeetingParticipant.ParticipantType.EXTERNAL)
                .status(MeetingParticipant.ParticipantStatus.INVITED)
                .build();
    }
}
