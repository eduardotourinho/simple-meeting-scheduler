package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.Meeting;
import dev.eduardo.scheduler.domain.entities.MeetingParticipant;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;

    @Transactional
    public Meeting saveMeeting(Meeting meeting) {
        return meetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public Meeting findById(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with ID: " + meetingId));
    }

    @Transactional
    public MeetingParticipant createInternalParticipant(Meeting meeting, User user) {
        log.info("Creating internal participant for user: {}", user.getEmail());
        return MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .participantType(MeetingParticipant.ParticipantType.INTERNAL)
                .status(MeetingParticipant.ParticipantStatus.INVITED)
                .build();
    }

    @Transactional
    public MeetingParticipant createExternalParticipant(Meeting meeting, String name, String email) {
        log.info("Creating external participant: {}", email);
        return MeetingParticipant.builder()
                .meeting(meeting)
                .externalEmail(email.toLowerCase())
                .externalName(name)
                .participantType(MeetingParticipant.ParticipantType.EXTERNAL)
                .status(MeetingParticipant.ParticipantStatus.INVITED)
                .build();
    }
}
