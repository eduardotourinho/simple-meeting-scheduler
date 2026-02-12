package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.TimeSlot;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public record TimeSlotSummary(
        UUID id,
        LocalTime startTime,
        LocalTime endTime,
        TimeSlot.SlotStatus status
) {
    public static TimeSlotSummary fromEntity(TimeSlot timeSlot, ZoneId userTimeZone) {
        return new TimeSlotSummary(
                timeSlot.getId(),
                timeSlot.getStartTime().atZone(userTimeZone).toLocalTime(),
                timeSlot.getEndTime().atZone(userTimeZone).toLocalTime(),
                timeSlot.getStatus()
        );
    }
}
