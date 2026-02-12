package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.DateSlots;
import dev.eduardo.scheduler.api.dto.PageableUserTimeSlotsResponse;
import dev.eduardo.scheduler.api.dto.TimeSlotSummary;
import dev.eduardo.scheduler.api.dto.UserInfo;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TimeSlotService timeSlotService;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public PageableUserTimeSlotsResponse getUserTimeSlotsPageable(UUID userId, 
                                                                  LocalDate startDate, 
                                                                  LocalDate endDate, 
                                                                  TimeSlot.SlotStatus status,
                                                                  int page,
                                                                  int size) {
        // Verify user exists and get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Get user's timezone
        ZoneId userTimeZone = ZoneId.of(user.getTimezone());
        
        // Fetch time slots based on filters
        List<TimeSlot> timeSlots = timeSlotService.fetchFilteredTimeSlots(userId, startDate, endDate, status, userTimeZone);
        
        // Group by date and convert to response format
        Map<LocalDate, List<TimeSlotSummary>> slotsByDate = timeSlots.stream()
                .collect(Collectors.groupingBy(
                        slot -> slot.getStartTime().atZone(userTimeZone).toLocalDate(),
                        Collectors.mapping(
                                slot -> TimeSlotSummary.fromEntity(slot, userTimeZone),
                                Collectors.toList()
                        )
                ));
        
        // Convert to DateSlots and sort by date
        List<DateSlots> allDateSlots = slotsByDate.entrySet().stream()
                .map(entry -> new DateSlots(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DateSlots::date))
                .toList();
        
        // Apply pagination by date
        int totalElements = allDateSlots.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        List<DateSlots> paginatedDateSlots = startIndex < totalElements ? 
                allDateSlots.subList(startIndex, endIndex) : List.of();
        
        // Create page info
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
}
