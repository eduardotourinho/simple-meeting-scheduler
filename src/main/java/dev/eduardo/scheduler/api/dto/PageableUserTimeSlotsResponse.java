package dev.eduardo.scheduler.api.dto;

import java.util.List;

public record PageableUserTimeSlotsResponse(
        UserInfo user,
        List<DateSlots> timeSlots,
        PageInfo pageInfo
) {
    public record PageInfo(
            int page,
            int size,
            int totalPages,
            long totalElements,
            boolean hasNext,
            boolean hasPrevious
    ) {}
}
