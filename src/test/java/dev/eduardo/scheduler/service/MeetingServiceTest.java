package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.Meeting;
import dev.eduardo.scheduler.domain.entities.MeetingParticipant;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepositoryMock;

    @InjectMocks
    private MeetingService meetingService;

    private final UUID meetingId = UUID.randomUUID();

    @Test
    void shouldSaveMeeting() {
        // Given
        var meeting = createTestMeeting();
        var savedMeeting = createSavedMeeting(meeting);

        when(meetingRepositoryMock.save(meeting)).thenReturn(savedMeeting);

        // When
        Meeting result = meetingService.saveMeeting(meeting);

        // Then
        assertNotNull(result);
        assertEquals(savedMeeting.getId(), result.getId());
        assertEquals(savedMeeting.getTitle(), result.getTitle());
        assertEquals(savedMeeting.getDescription(), result.getDescription());

        verify(meetingRepositoryMock).save(meeting);
    }

    @Test
    void shouldFindMeetingById() {
        // Given
        var meeting = createTestMeeting();
        when(meetingRepositoryMock.findById(meetingId)).thenReturn(Optional.of(meeting));

        // When
        Meeting result = meetingService.findById(meetingId);

        // Then
        assertNotNull(result);
        assertEquals(meeting.getId(), result.getId());
        assertEquals(meeting.getTitle(), result.getTitle());

        verify(meetingRepositoryMock).findById(meetingId);
    }

    @Test
    void shouldThrowException_WhenMeetingNotFound() {
        // Given
        when(meetingRepositoryMock.findById(meetingId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> meetingService.findById(meetingId));

        // Then
        assertEquals("Meeting not found with ID: " + meetingId, exception.getMessage());
        verify(meetingRepositoryMock).findById(meetingId);
    }
    
    @Test
    void shouldCreateInternalParticipant() {
        // Given
        var meeting = createTestMeeting();
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .timezone("UTC")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        // When
        MeetingParticipant result = meetingService.createInternalParticipant(meeting, user);

        // Then
        assertNotNull(result);
        assertEquals(meeting, result.getMeeting());
        assertEquals(user, result.getUser());
        assertEquals(MeetingParticipant.ParticipantType.INTERNAL, result.getParticipantType());
        assertEquals(MeetingParticipant.ParticipantStatus.INVITED, result.getStatus());
        assertNull(result.getExternalEmail());
        assertNull(result.getExternalName());
    }

    @Test
    void shouldCreateExternalParticipant() {
        // Given
        var meeting = createTestMeeting();
        var name = "External User";
        var email = "external@example.com";

        // When
        MeetingParticipant result = meetingService.createExternalParticipant(meeting, name, email);

        // Then
        assertNotNull(result);
        assertEquals(meeting, result.getMeeting());
        assertEquals(MeetingParticipant.ParticipantType.EXTERNAL, result.getParticipantType());
        assertEquals(MeetingParticipant.ParticipantStatus.INVITED, result.getStatus());
        assertEquals(email.toLowerCase(), result.getExternalEmail());
        assertEquals(name, result.getExternalName());
        assertNull(result.getUser());
    }

    private Meeting createTestMeeting() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .timezone("UTC")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        
        return Meeting.builder()
                .id(meetingId)
                .title("Test Meeting")
                .description("Test Description")
                .organizer(user)
                .participants(new java.util.ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Meeting createSavedMeeting(Meeting meeting) {
        return Meeting.builder()
                .id(UUID.randomUUID())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .organizer(meeting.getOrganizer())
                .participants(meeting.getParticipants())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
