package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateTimeSlotRequest(
        
        @NotNull(message = "Start time is required")
        Instant startTime,
        
        @NotNull(message = "End time is required") 
        Instant endTime,
        
        TimeSlot.SlotStatus status  // Optional
) {
    public UpdateTimeSlotRequest {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }
}
