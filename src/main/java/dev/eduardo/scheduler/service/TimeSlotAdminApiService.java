package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.BulkCreateTimeSlotsResponse;
import dev.eduardo.scheduler.api.dto.CreateTimeSlotRequest;
import dev.eduardo.scheduler.api.dto.TimeSlotResponse;
import dev.eduardo.scheduler.api.dto.UpdateTimeSlotRequest;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.TimeSlotOverlapException;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSlotAdminApiService {

    private final TimeSlotService timeSlotService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TimeSlotResponse getTimeSlot(UUID timeSlotId) {
        TimeSlot timeSlot = timeSlotService.findById(timeSlotId);
        return TimeSlotResponse.fromEntity(timeSlot);
    }

    @Transactional
    public BulkCreateTimeSlotsResponse createTimeSlots(@Valid CreateTimeSlotRequest request) {
        // Check if user exists
        var user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + request.userId()));

        List<TimeSlotResponse> createdSlots = new java.util.ArrayList<>();
        
        for (var slotData : request.slots()) {
            // Validate time range
            if (!slotData.endTime().isAfter(slotData.startTime())) {
                throw new IllegalArgumentException("End time must be after start time for slot starting at " + slotData.startTime());
            }

            // Check for overlapping time slots
            if (timeSlotService.hasOverlappingSlots(user.getId(), slotData.startTime(), slotData.endTime())) {
                throw new TimeSlotOverlapException(
                        String.format("Time slot overlaps with existing slot for user %s from %s to %s",
                                user.getEmail(), slotData.startTime(), slotData.endTime()));
            }

            // Create time slot
            var status = slotData.status() != null ? slotData.status() : TimeSlot.SlotStatus.AVAILABLE;

            var savedTimeSlot = timeSlotService.createSlot(TimeSlot.builder()
                    .user(user)
                    .startTime(slotData.startTime())
                    .endTime(slotData.endTime())
                    .status(status)
                    .build());

            createdSlots.add(TimeSlotResponse.fromEntity(savedTimeSlot));
        }

        log.info("Created {} time slots for user {}", createdSlots.size(), user.getEmail());

        return new BulkCreateTimeSlotsResponse(createdSlots, createdSlots.size());
    }


    @Transactional
    public TimeSlotResponse updateTimeSlot(UUID timeSlotId, @Valid UpdateTimeSlotRequest request) {
        var timeSlot = timeSlotService.findById(timeSlotId);

        // Validate time range
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Check for overlapping time slots (excluding current slot)
        if (timeSlotService.hasOverlappingSlots(timeSlot.getUser().getId(), request.startTime(), request.endTime(), timeSlot)) {
            throw new TimeSlotOverlapException(
                    String.format("Updated time slot would overlap with existing slot for user %s from %s to %s",
                            timeSlot.getUser().getEmail(), request.startTime(), request.endTime()));
        }

        // Update time slot
        timeSlot.setStartTime(request.startTime());
        timeSlot.setEndTime(request.endTime());

        if (request.status() != null) {
            timeSlot.setStatus(request.status());
        }

        var updatedTimeSlot = timeSlotService.updateSlot(timeSlot);
        log.info("Updated time slot {} for user {}", timeSlotId, timeSlot.getUser().getEmail());

        return TimeSlotResponse.fromEntity(updatedTimeSlot);
    }

    @Transactional
    public void deleteTimeSlot(UUID timeSlotId) {
        var timeSlot = timeSlotService.findById(timeSlotId);

        if (timeSlot.getStatus() == TimeSlot.SlotStatus.BOOKED) {
            log.warn("Attempting to delete booked time slot {} for user {}",
                    timeSlotId, timeSlot.getUser().getEmail());
            throw new IllegalArgumentException("Attempting to delete booked time slot " + timeSlotId);
        }

        timeSlotService.removeSlot(timeSlot);
        log.info("Deleted time slot {} for user {}", timeSlotId, timeSlot.getUser().getEmail());
    }

}
