package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.*;
import dev.eduardo.scheduler.domain.entities.Meeting;
import dev.eduardo.scheduler.domain.entities.MeetingParticipant;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.service.exception.TimeSlotNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TimeSlotService timeSlotService;
    private final UserService userService;
    private final MeetingService meetingService;


    @Transactional(readOnly = true)
    @Cacheable(
            value = "userTimeSlotsPageable",
            key = "T(java.util.Objects).hash(#userId, #startDate, #endDate, #status, #page, #size)"
    )
    public PageableUserTimeSlotsResponse getUserTimeSlotsPageable(UUID userId, 
                                                                  LocalDate startDate, 
                                                                  LocalDate endDate, 
                                                                  TimeSlot.SlotStatus status,
                                                                  int page,
                                                                  int size) {
        var user = userService.findById(userId);

        var userTimeZone = ZoneId.of(user.getTimezone());

        var timeSlots = timeSlotService.fetchFilteredTimeSlots(userId, startDate, endDate, status, userTimeZone);
        
        Map<LocalDate, List<TimeSlotSummary>> slotsByDate = timeSlots.stream()
                .collect(Collectors.groupingBy(
                        slot -> slot.getStartTime().atZone(userTimeZone).toLocalDate(),
                        Collectors.mapping(
                                slot -> TimeSlotSummary.fromEntity(slot, userTimeZone),
                                Collectors.toList()
                        )
                ));
        
        var allDateSlots = slotsByDate.entrySet().stream()
                .map(entry -> new DateSlots(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DateSlots::date))
                .toList();
        
        int totalElements = allDateSlots.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<DateSlots> paginatedDateSlots = startIndex < totalElements ?
                new ArrayList<>(allDateSlots.subList(startIndex, endIndex)) : new ArrayList<>();
        
        PageableUserTimeSlotsResponse.PageInfo pageInfo = new PageableUserTimeSlotsResponse.PageInfo(
                page,
                size,
                totalPages,
                totalElements,
                page < totalPages - 1,
                page > 0
        );
        
        return new PageableUserTimeSlotsResponse(UserInfo.fromEntity(user), paginatedDateSlots, pageInfo);
    }

    @Transactional
    @CacheEvict(value = "userTimeSlotsPageable", allEntries = true)
    public CreateMeetingResponse createMeeting(UUID timeSlotId, CreateMeetingRequest request) {
        log.info("Creating meeting for time slot: {} with {} participants", 
                timeSlotId, request.participants().size());

        TimeSlot timeSlot = timeSlotService.findById(timeSlotId);
        
        if (timeSlot.getStatus() != TimeSlot.SlotStatus.AVAILABLE) {
            throw new TimeSlotNotAvailableException("Time slot is not available for booking");
        }
        
        var organizerUser = timeSlot.getUser();

        var meeting = Meeting.builder()
                .timeSlot(timeSlot)
                .title(request.title())
                .description(request.description()) // Can be null/empty
                .organizer(organizerUser)
                .participants(new ArrayList<>())
                .build();

        var savedMeeting = meetingService.saveMeeting(meeting);

        var participants = request.participants().stream()
                .map(participantRequest -> createParticipant(savedMeeting, participantRequest))
                .toList();

        savedMeeting.getParticipants().addAll(participants);

        timeSlot.setStatus(TimeSlot.SlotStatus.BOOKED);
        timeSlotService.updateSlot(timeSlot);
        
        log.info("Meeting created successfully with ID: {} and {} participants", 
                savedMeeting.getId(), savedMeeting.getParticipants().size());
        return CreateMeetingResponse.fromEntity(savedMeeting);
    }
    
    private MeetingParticipant createParticipant(Meeting meeting, CreateMeetingRequest.ParticipantRequest participantRequest) {
        var email = participantRequest.email();
        var name = participantRequest.name();

        var existingUser = userService.findByEmail(email);
        
        if (existingUser.isPresent()) {
            return meetingService.createInternalParticipant(meeting, existingUser.get());
        } else {
            return meetingService.createExternalParticipant(meeting, name, email);
        }
    }
}
