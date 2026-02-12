package dev.eduardo.scheduler.api.dto;

import java.util.List;

public record BulkCreateTimeSlotsResponse(
        List<TimeSlotResponse> createdSlots,
        int createdCount
) {
}
