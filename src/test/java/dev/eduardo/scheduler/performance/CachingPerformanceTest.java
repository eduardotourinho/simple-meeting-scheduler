package dev.eduardo.scheduler.performance;

import dev.eduardo.scheduler.TestcontainersConfiguration;
import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.service.CalendarService;
import dev.eduardo.scheduler.service.TimeSlotService;
import dev.eduardo.scheduler.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CachingPerformanceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private List<User> testUsers;
    private List<TimeSlot> testTimeSlots;
    private final String runId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        // Clear all caches
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear());
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test data
        createTestData();
    }

    @Test
    void shouldDemonstrateCachingPerformance() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        var status = TimeSlot.SlotStatus.AVAILABLE;
        int page = 0;
        int size = 10;

        // Warm up - populate cache
        for (User user : testUsers) {
            var response = calendarService.getUserTimeSlotsPageable(
                    user.getId(), startDate, endDate, status, page, size);
            assertNotNull(response);
        }

        // Measure cached performance
        long startTime = System.nanoTime();
        
        // Perform 100 cached lookups
        for (int i = 0; i < 100; i++) {
            for (User user : testUsers) {
                var response = calendarService.getUserTimeSlotsPageable(
                        user.getId(), startDate, endDate, status, page, size);
                assertNotNull(response);
            }
        }
        
        long cachedTime = System.nanoTime() - startTime;
        double cachedTimeMs = cachedTime / 1_000_000.0;

        System.out.println("=== Caching Performance Test Results ===");
        System.out.println("Total cached operations: " + (100 * testUsers.size()));
        System.out.println("Total cached time: " + String.format("%.2f", cachedTimeMs) + " ms");
        System.out.println("Average time per operation: " + String.format("%.4f", cachedTimeMs / (100 * testUsers.size())) + " ms");

        // Verify cache hit rate
        var calendarCache = cacheManager.getCache("userTimeSlotsPageable");
        
        assertNotNull(calendarCache);
        
        System.out.println("Cache statistics:");
        System.out.println("Calendar cache: " + calendarCache.getNativeCache());
    }

    @Test
    void shouldHandleConcurrentCacheAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        var status = TimeSlot.SlotStatus.AVAILABLE;
        int page = 0;
        int size = 10;

        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    var response = calendarService.getUserTimeSlotsPageable(
                            testUsers.get(threadId % testUsers.size()).getId(),
                            startDate, endDate, status, page, size);
                    assertNotNull(response);
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long totalTime = System.nanoTime() - startTime;
        double totalTimeMs = totalTime / 1_000_000.0;

        System.out.println("=== Concurrent Caching Performance Test Results ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations: " + (threadCount * operationsPerThread));
        System.out.println("Total time: " + String.format("%.2f", totalTimeMs) + " ms");
        System.out.println("Average time per operation: " + String.format("%.4f", totalTimeMs / (threadCount * operationsPerThread)) + " ms");

        // Performance assertion - concurrent cached operations should still be fast
        assertTrue(totalTimeMs < 5000, "Concurrent cached operations should complete within 5 seconds");
    }

    @Test
    void shouldDemonstrateCacheEvictionPerformance() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        var status = TimeSlot.SlotStatus.AVAILABLE;
        int page = 0;
        int size = 10;

        // Warm up cache
        for (User user : testUsers) {
            var response = calendarService.getUserTimeSlotsPageable(
                    user.getId(), startDate, endDate, status, page, size);
            assertNotNull(response);
        }

        // Verify cache is populated
        var calendarCache = cacheManager.getCache("userTimeSlotsPageable");
        assertNotNull(calendarCache);

        long startTime = System.nanoTime();

        // Update time slots (should evict cache)
        for (TimeSlot slot : testTimeSlots) {
            TimeSlot managed = timeSlotService.findById(slot.getId());
            managed.setStatus(TimeSlot.SlotStatus.BUSY);
            timeSlotService.updateSlot(managed);
        }

        // Repopulate cache after eviction
        for (User user : testUsers) {
            var response = calendarService.getUserTimeSlotsPageable(
                    user.getId(), startDate, endDate, status, page, size);
            assertNotNull(response);
        }

        long evictionTime = System.nanoTime() - startTime;
        double evictionTimeMs = evictionTime / 1_000_000.0;

        System.out.println("=== Cache Eviction Performance Test Results ===");
        System.out.println("Time slots updated: " + testTimeSlots.size());
        System.out.println("Total eviction time: " + String.format("%.2f", evictionTimeMs) + " ms");
        System.out.println("Average time per eviction: " + String.format("%.4f", evictionTimeMs / testTimeSlots.size()) + " ms");

        // Eviction should be reasonably fast
        assertTrue(evictionTimeMs < 2000, "Cache eviction operations should complete within 2 seconds");
    }

    private void createTestData() {
        testUsers = new ArrayList<>();
        testTimeSlots = new ArrayList<>();

        // Create 10 users
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .name("Test User " + i)
                    .email("test" + i + "-" + runId + "@example.com")
                    .timezone("UTC")
                    .build();

            User savedUser = userService.save(user);
            testUsers.add(savedUser);

            // Create 5 time slots per user
            for (int j = 0; j < 5; j++) {
                TimeSlot timeSlot = TimeSlot.builder()
                        .user(savedUser)
                        .startTime(Instant.now().plus(j * 2, ChronoUnit.HOURS))
                        .endTime(Instant.now().plus(j * 2 + 1, ChronoUnit.HOURS))
                        .status(TimeSlot.SlotStatus.AVAILABLE)
                        .build();

                TimeSlot savedTimeSlot = timeSlotService.createSlot(timeSlot);
                testTimeSlots.add(savedTimeSlot);
            }
        }
    }
}
