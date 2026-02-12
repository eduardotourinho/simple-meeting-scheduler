package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.api.dto.CreateMeetingRequest;
import dev.eduardo.scheduler.api.dto.CreateMeetingResponse;
import dev.eduardo.scheduler.api.dto.PageableUserTimeSlotsResponse;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final CalendarService timeSlotService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageableUserTimeSlotsResponse> getUserTimeSlots(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TimeSlot.SlotStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageableUserTimeSlotsResponse timeSlots = timeSlotService.getUserTimeSlotsPageable(
                userId, startDate, endDate, status, page, size);
        return ResponseEntity.ok(timeSlots);
    }

    @PostMapping("/{timeSlotId}/meetings")
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @PathVariable UUID timeSlotId,
            @Valid @RequestBody CreateMeetingRequest request) {
        CreateMeetingResponse response = timeSlotService.createMeeting(timeSlotId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
