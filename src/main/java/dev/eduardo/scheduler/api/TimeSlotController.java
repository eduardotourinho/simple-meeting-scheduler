package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.api.dto.CreateMeetingRequest;
import dev.eduardo.scheduler.api.dto.CreateMeetingResponse;
import dev.eduardo.scheduler.api.dto.PageableUserTimeSlotsResponse;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-slots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Time Slots", description = "Public operations for viewing time slots and creating meetings")
public class TimeSlotController {

    private final CalendarService timeSlotService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user time slots", description = "Retrieves paginated time slots for a specific user with optional filtering by date range and status")
    public ResponseEntity<PageableUserTimeSlotsResponse> getUserTimeSlots(
            @PathVariable @Parameter(description = "User ID") UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            @Parameter(description = "Start date filter (ISO date format)") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            @Parameter(description = "End date filter (ISO date format)") LocalDate endDate,
            @RequestParam(required = false) 
            @Parameter(description = "Time slot status filter") TimeSlot.SlotStatus status,
            @RequestParam(defaultValue = "0") 
            @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "10") 
            @Parameter(description = "Page size") int size) {

        log.info("Getting time slots for user {} with filters - startDate: {}, endDate: {}, status: {}, page: {}, size: {}", 
                userId, startDate, endDate, status, page, size);
        
        PageableUserTimeSlotsResponse timeSlots = timeSlotService.getUserTimeSlotsPageable(
                userId, startDate, endDate, status, page, size);
        return ResponseEntity.ok(timeSlots);
    }

    @PostMapping("/{timeSlotId}/meetings")
    @Operation(summary = "Create meeting", description = "Creates a new meeting by booking an available time slot")
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @PathVariable @Parameter(description = "Time slot ID to book") UUID timeSlotId,
            @Valid @RequestBody CreateMeetingRequest request) {
        
        log.info("Creating meeting for time slot {} with request: {}", timeSlotId, request);
        CreateMeetingResponse response = timeSlotService.createMeeting(timeSlotId, request);
        log.info("Successfully created meeting with ID: {}", response.meetingId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
