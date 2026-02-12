package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.Meeting;
import dev.eduardo.scheduler.domain.entities.MeetingParticipant;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateMeetingResponse(
        UUID meetingId,
        UUID timeSlotId,
        String title,
        String description,
        UUID organizerId,
        String organizerEmail,
        Instant startTime,
        Instant endTime,
        List<ParticipantInfo> participants,
        LocalDateTime createdAt
) {
    
    public record ParticipantInfo(
            UUID participantId,
            String name,
            String email,
            MeetingParticipant.ParticipantType type,
            MeetingParticipant.ParticipantStatus status
    ) {}
    
    public static CreateMeetingResponse fromEntity(Meeting meeting) {
        List<ParticipantInfo> participants = meeting.getParticipants().stream()
                .map(participant -> new ParticipantInfo(
                        participant.getId(),
                        participant.getUser() != null ? participant.getUser().getName() : participant.getExternalName(),
                        participant.getUser() != null ? participant.getUser().getEmail() : participant.getExternalEmail(),
                        participant.getParticipantType(),
                        participant.getStatus()
                ))
                .toList();
        
        return new CreateMeetingResponse(
                meeting.getId(),
                meeting.getTimeSlot().getId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getOrganizer().getId(),
                meeting.getOrganizer().getEmail(),
                meeting.getTimeSlot().getStartTime(),
                meeting.getTimeSlot().getEndTime(),
                participants,
                meeting.getCreatedAt()
        );
    }
}
