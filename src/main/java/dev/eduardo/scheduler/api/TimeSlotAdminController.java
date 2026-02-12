package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.api.dto.BulkCreateTimeSlotsResponse;
import dev.eduardo.scheduler.api.dto.CreateTimeSlotRequest;
import dev.eduardo.scheduler.api.dto.TimeSlotResponse;
import dev.eduardo.scheduler.api.dto.UpdateTimeSlotRequest;
import dev.eduardo.scheduler.service.TimeSlotAdminApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/admin/time-slots")
@RequiredArgsConstructor
public class TimeSlotAdminController {

    private final TimeSlotAdminApiService timeSlotAdminService;

    @PostMapping
    public ResponseEntity<BulkCreateTimeSlotsResponse> createTimeSlots(@Valid @RequestBody CreateTimeSlotRequest request) {
        var response = timeSlotAdminService.createTimeSlots(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{timeSlotId}")
    public ResponseEntity<TimeSlotResponse> getTimeSlot(@PathVariable UUID timeSlotId) {
        var timeSlot = timeSlotAdminService.getTimeSlot(timeSlotId);
        return ResponseEntity.ok(timeSlot);
    }

    @PutMapping("/{timeSlotId}")
    public ResponseEntity<TimeSlotResponse> updateTimeSlot(
            @PathVariable UUID timeSlotId,
            @Valid @RequestBody UpdateTimeSlotRequest request) {
        var timeSlot = timeSlotAdminService.updateTimeSlot(timeSlotId, request);
        return ResponseEntity.ok(timeSlot);
    }

    @DeleteMapping("/{timeSlotId}")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable UUID timeSlotId) {
        timeSlotAdminService.deleteTimeSlot(timeSlotId);
        return ResponseEntity.noContent().build();
    }
}
