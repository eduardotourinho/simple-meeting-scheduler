package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.repository.TimeSlotRepository;
import dev.eduardo.scheduler.service.exception.TimeSlotNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Transactional
    public TimeSlot createSlot(TimeSlot timeSlot) {
        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public TimeSlot updateSlot(TimeSlot timeSlot) {
        return timeSlotRepository.save(timeSlot);
    }

    public void removeSlot(TimeSlot timeSlot) {
        timeSlotRepository.delete(timeSlot);
    }

    @Transactional(readOnly = true)
    public TimeSlot findById(UUID timeSlotId) {
        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new TimeSlotNotFoundException("Time slot not found with ID: " + timeSlotId));
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> fetchFilteredTimeSlots(UUID userId,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  TimeSlot.SlotStatus status,
                                                  ZoneId userTimeZone) {
        // Convert LocalDate to Instant for database queries
        Instant startInstant = null;
        Instant endInstant = null;

        if (startDate != null) {
            startInstant = startDate.atStartOfDay(userTimeZone).toInstant();
        }
        if (endDate != null) {
            endInstant = endDate.plusDays(1).atStartOfDay(userTimeZone).toInstant();
        }

        // Choose appropriate repository method based on filters
        if (startInstant != null && endInstant != null && status != null) {
            return timeSlotRepository.findByUserIdAndStatusAndTimeRange(userId, status, startInstant, endInstant);
        } else if (startInstant != null && endInstant != null) {
            return timeSlotRepository.findByUserIdAndTimeRange(userId, startInstant, endInstant);
        } else if (status != null) {
            return timeSlotRepository.findByUserIdAndStatusOrderByStartTime(userId, status);
        } else {
            return timeSlotRepository.findByUserIdOrderByStartTime(userId);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasOverlappingSlots(UUID userId, Instant startTime, Instant endTime) {
        return timeSlotRepository.existsOverlappingSlot(userId, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public boolean hasOverlappingSlots(UUID userId, Instant startTime, Instant endTime, TimeSlot timeSlot) {
        return timeSlotRepository.existsOverlappingSlot(userId, startTime, endTime, timeSlot.getId());
    }
}
