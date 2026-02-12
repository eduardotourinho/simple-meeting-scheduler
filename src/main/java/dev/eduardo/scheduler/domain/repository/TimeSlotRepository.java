package dev.eduardo.scheduler.domain.repository;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {
    
    /**
     * Find all time slots for a specific user
     */
    List<TimeSlot> findByUserIdOrderByStartTime(UUID userId);
    
    /**
     * Find time slots by status for a user
     */
    List<TimeSlot> findByUserIdAndStatusOrderByStartTime(UUID userId, TimeSlot.SlotStatus status);
    
    /**
     * Find time slots for a user within a time range
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.user.id = :userId " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findByUserIdAndTimeRange(@Param("userId") UUID userId,
                                           @Param("startTime") Instant startTime,
                                           @Param("endTime") Instant endTime);
    
    /**
     * Find time slots for a user within a time range and status
     */
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.user.id = :userId " +
           "AND ts.status = :status " +
           "AND ts.startTime >= :startTime AND ts.endTime <= :endTime " +
           "ORDER BY ts.startTime")
    List<TimeSlot> findByUserIdAndStatusAndTimeRange(@Param("userId") UUID userId,
                                                    @Param("status") TimeSlot.SlotStatus status,
                                                    @Param("startTime") Instant startTime,
                                                    @Param("endTime") Instant endTime);
    
    /**
     * Check for overlapping time slots for a user (excluding a specific slot ID)
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeSlot ts WHERE ts.user.id = :userId " +
           "AND ts.id != :excludeId " +
           "AND ((ts.startTime < :endTime AND ts.endTime > :startTime))")
    boolean existsOverlappingSlot(@Param("userId") UUID userId,
                                  @Param("startTime") Instant startTime,
                                  @Param("endTime") Instant endTime,
                                  @Param("excludeId") UUID excludeId);
    
    /**
     * Check for overlapping time slots for a user (for new slots)
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeSlot ts WHERE ts.user.id = :userId " +
           "AND ((ts.startTime < :endTime AND ts.endTime > :startTime))")
    boolean existsOverlappingSlot(@Param("userId") UUID userId,
                                  @Param("startTime") Instant startTime,
                                  @Param("endTime") Instant endTime);
}
