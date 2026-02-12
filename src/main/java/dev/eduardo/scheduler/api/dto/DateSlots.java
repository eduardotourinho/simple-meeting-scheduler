package dev.eduardo.scheduler.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DateSlots(
        LocalDate date,
        List<TimeSlotSummary> slots
) {}
