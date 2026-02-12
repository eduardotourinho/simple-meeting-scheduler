package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.TimeSlot;

import java.time.Instant;
import java.util.UUID;

public record TimeSlotResponse(
        UUID id,
        UUID userId,
        String userEmail,
        Instant startTime,
        Instant endTime,
        TimeSlot.SlotStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TimeSlotResponse fromEntity(TimeSlot timeSlot) {
        return new TimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getUser().getId(),
                timeSlot.getUser().getEmail(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getStatus(),
                timeSlot.getCreatedAt(),
                timeSlot.getUpdatedAt()
        );
    }
}
