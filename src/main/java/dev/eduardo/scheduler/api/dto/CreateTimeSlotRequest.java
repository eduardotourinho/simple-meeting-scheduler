package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CreateTimeSlotRequest(
        @NotNull(message = "User ID is required")
        UUID userId,
        
        @NotEmpty(message = "Slots cannot be empty")
        List<TimeSlotData> slots
) {

    @Valid
    @Builder
    public record TimeSlotData(
            @NotNull(message = "Start time is required")
            Instant startTime,
            
            @NotNull(message = "End time is required")
            Instant endTime,
            
            TimeSlot.SlotStatus status
    ) {
    }
}
