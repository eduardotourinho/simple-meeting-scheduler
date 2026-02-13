package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.api.dto.BulkCreateTimeSlotsResponse;
import dev.eduardo.scheduler.api.dto.CreateTimeSlotRequest;
import dev.eduardo.scheduler.api.dto.TimeSlotResponse;
import dev.eduardo.scheduler.api.dto.UpdateTimeSlotRequest;
import dev.eduardo.scheduler.service.AuthService;
import dev.eduardo.scheduler.service.TimeSlotAdminApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/admin/time-slots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Time Slot Admin", description = "Administrative operations for time slots (requires authorization)")
@SecurityRequirement(name = "bearerAuth")
public class TimeSlotAdminController {

    private final AuthService authService;
    private final TimeSlotAdminApiService timeSlotAdminService;


    @PostMapping
    @Operation(summary = "Create time slots", description = "Creates multiple time slots. Requires valid Authorization token.")
    public ResponseEntity<BulkCreateTimeSlotsResponse> createTimeSlots(
            @Valid @RequestBody CreateTimeSlotRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = authService.getAuthenticatedUserId();
        log.info("Creating time slots for user {} - authorized request from: {}", userId, httpRequest.getRemoteAddr());
        var response = timeSlotAdminService.createTimeSlots(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{timeSlotId}")
    @Operation(summary = "Get time slot", description = "Retrieves a specific time slot by ID. Requires valid Authorization token.")
    public ResponseEntity<TimeSlotResponse> getTimeSlot(
            @PathVariable @Parameter(description = "Time slot ID") UUID timeSlotId,
            HttpServletRequest httpRequest) {

        UUID userId = authService.getAuthenticatedUserId();
        log.info("Getting time slot {} for user {} - authorized request from: {}", timeSlotId, userId, httpRequest.getRemoteAddr());
        var timeSlot = timeSlotAdminService.getTimeSlot(timeSlotId, userId);
        return ResponseEntity.ok(timeSlot);
    }

    @PutMapping("/{timeSlotId}")
    @Operation(summary = "Update time slot", description = "Updates an existing time slot. Requires valid Authorization token.")
    public ResponseEntity<TimeSlotResponse> updateTimeSlot(
            @PathVariable @Parameter(description = "Time slot ID") UUID timeSlotId,
            @Valid @RequestBody UpdateTimeSlotRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = authService.getAuthenticatedUserId();
        log.info("Updating time slot {} for user {} - authorized request from: {}", timeSlotId, userId, httpRequest.getRemoteAddr());
        var timeSlot = timeSlotAdminService.updateTimeSlot(timeSlotId, request, userId);
        return ResponseEntity.ok(timeSlot);
    }

    @DeleteMapping("/{timeSlotId}")
    @Operation(summary = "Delete time slot", description = "Deletes a time slot by ID. Requires valid Authorization token.")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable @Parameter(description = "Time slot ID") UUID timeSlotId,
            HttpServletRequest httpRequest) {

        UUID userId = authService.getAuthenticatedUserId();
        log.info("Deleting time slot {} for user {} - authorized request from: {}", timeSlotId, userId, httpRequest.getRemoteAddr());
        timeSlotAdminService.deleteTimeSlot(timeSlotId, userId);
        return ResponseEntity.noContent().build();
    }
}
