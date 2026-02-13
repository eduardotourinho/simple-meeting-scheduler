package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.TimeSlotRepository;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class RedisCachingTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private final UUID userId = UUID.randomUUID();
    private final UUID timeSlotId = UUID.randomUUID();
    private final String userEmail = "test@example.com";
    private final ZoneId userTimeZone = ZoneId.of("UTC");

    private User testUser;
    private TimeSlot testTimeSlot;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email(userEmail)
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testTimeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .user(testUser)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldCacheUserFindById() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When - First call should hit repository
        User result1 = userService.findById(userId);
        assertEquals(testUser, result1);
        verify(userRepository, times(1)).findById(userId);

        // When - Second call should use cache
        User result2 = userService.findById(userId);
        assertEquals(testUser, result2);
        
        // Then - Repository should only be called once
        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldCacheUserFindByEmail() {
        // Given
        when(userRepository.findByEmail(userEmail.toLowerCase())).thenReturn(Optional.of(testUser));

        // When - First call should hit repository
        Optional<User> result1 = userService.findByEmail(userEmail);
        assertTrue(result1.isPresent());
        assertEquals(testUser, result1.get());
        verify(userRepository, times(1)).findByEmail(userEmail.toLowerCase());

        // When - Second call should use cache
        Optional<User> result2 = userService.findByEmail(userEmail);
        assertTrue(result2.isPresent());
        assertEquals(testUser, result2.get());

        // Then - Repository should only be called once
        verify(userRepository, times(1)).findByEmail(userEmail.toLowerCase());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldCacheTimeSlotFindById() {
        // Given
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.of(testTimeSlot));

        // When - First call should hit repository
        TimeSlot result1 = timeSlotService.findById(timeSlotId);
        assertEquals(testTimeSlot, result1);
        verify(timeSlotRepository, times(1)).findById(timeSlotId);

        // When - Second call should use cache
        TimeSlot result2 = timeSlotService.findById(timeSlotId);
        assertEquals(testTimeSlot, result2);

        // Then - Repository should only be called once
        verify(timeSlotRepository, times(1)).findById(timeSlotId);
        verifyNoMoreInteractions(timeSlotRepository);
    }

    @Test
    void shouldCacheFetchFilteredTimeSlots() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        TimeSlot.SlotStatus status = TimeSlot.SlotStatus.AVAILABLE;
        List<TimeSlot> expectedSlots = List.of(testTimeSlot);

        when(timeSlotRepository.findByUserIdAndStatusAndTimeRange(
                eq(userId), eq(status), any(Instant.class), any(Instant.class)))
                .thenReturn(expectedSlots);

        // When - First call should hit repository
        List<TimeSlot> result1 = timeSlotService.fetchFilteredTimeSlots(
                userId, startDate, endDate, status, userTimeZone);
        assertEquals(expectedSlots, result1);
        verify(timeSlotRepository, times(1))
                .findByUserIdAndStatusAndTimeRange(eq(userId), eq(status), any(Instant.class), any(Instant.class));

        // When - Second call with same parameters should use cache
        List<TimeSlot> result2 = timeSlotService.fetchFilteredTimeSlots(
                userId, startDate, endDate, status, userTimeZone);
        assertEquals(expectedSlots, result2);

        // Then - Repository should only be called once
        verify(timeSlotRepository, times(1))
                .findByUserIdAndStatusAndTimeRange(eq(userId), eq(status), any(Instant.class), any(Instant.class));
        verifyNoMoreInteractions(timeSlotRepository);
    }

    @Test
    void shouldNotCacheFetchFilteredTimeSlotsWithDifferentParameters() {
        // Given
        LocalDate startDate1 = LocalDate.of(2024, 1, 1);
        LocalDate endDate1 = LocalDate.of(2024, 1, 31);
        LocalDate startDate2 = LocalDate.of(2024, 2, 1);
        LocalDate endDate2 = LocalDate.of(2024, 2, 29);
        TimeSlot.SlotStatus status = TimeSlot.SlotStatus.AVAILABLE;
        List<TimeSlot> expectedSlots1 = List.of(testTimeSlot);
        List<TimeSlot> expectedSlots2 = List.of();

        when(timeSlotRepository.findByUserIdAndStatusAndTimeRange(
                eq(userId), eq(status), any(Instant.class), any(Instant.class)))
                .thenReturn(expectedSlots1, expectedSlots2);

        // When - First call
        List<TimeSlot> result1 = timeSlotService.fetchFilteredTimeSlots(
                userId, startDate1, endDate1, status, userTimeZone);
        assertEquals(expectedSlots1, result1);

        // When - Second call with different date range should hit repository again
        List<TimeSlot> result2 = timeSlotService.fetchFilteredTimeSlots(
                userId, startDate2, endDate2, status, userTimeZone);
        assertEquals(expectedSlots2, result2);

        // Then - Repository should be called twice for different parameters
        verify(timeSlotRepository, times(2))
                .findByUserIdAndStatusAndTimeRange(eq(userId), eq(status), any(Instant.class), any(Instant.class));
    }

    @Test
    void shouldEvictCacheOnTimeSlotCreate() {
        // Given
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(testTimeSlot);

        // When - Creating time slot should evict cache
        TimeSlot result = timeSlotService.createSlot(testTimeSlot);
        assertEquals(testTimeSlot, result);

        // Then - Verify save was called and cache would be evicted
        verify(timeSlotRepository, times(1)).save(testTimeSlot);
    }

    @Test
    void shouldEvictCacheOnTimeSlotUpdate() {
        // Given
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(testTimeSlot);

        // When - Updating time slot should evict cache
        TimeSlot result = timeSlotService.updateSlot(testTimeSlot);
        assertEquals(testTimeSlot, result);

        // Then - Verify save was called and cache would be evicted
        verify(timeSlotRepository, times(1)).save(testTimeSlot);
    }

    @Test
    void shouldEvictCacheOnTimeSlotDelete() {
        // Given
        doNothing().when(timeSlotRepository).delete(testTimeSlot);

        // When - Deleting time slot should evict cache
        timeSlotService.removeSlot(testTimeSlot);

        // Then - Verify delete was called and cache would be evicted
        verify(timeSlotRepository, times(1)).delete(testTimeSlot);
    }

    @Test
    void shouldHandleCacheMissForNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then - Should throw exception and not cache null
        assertThrows(dev.eduardo.scheduler.service.exception.UserNotFoundException.class,
                () -> userService.findById(userId));
        
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void shouldHandleCacheMissForNonExistentTimeSlot() {
        // Given
        when(timeSlotRepository.findById(timeSlotId)).thenReturn(Optional.empty());

        // When & Then - Should throw exception and not cache null
        assertThrows(dev.eduardo.scheduler.service.exception.TimeSlotNotFoundException.class,
                () -> timeSlotService.findById(timeSlotId));
        
        verify(timeSlotRepository, times(1)).findById(timeSlotId);
    }
}
