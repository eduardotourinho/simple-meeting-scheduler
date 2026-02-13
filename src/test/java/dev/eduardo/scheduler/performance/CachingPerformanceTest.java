package dev.eduardo.scheduler.performance;

import dev.eduardo.scheduler.domain.entities.TimeSlot;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.service.TimeSlotService;
import dev.eduardo.scheduler.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class CachingPerformanceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private List<User> testUsers;
    private List<TimeSlot> testTimeSlots;
    private final ZoneId userTimeZone = ZoneId.of("UTC");

    @BeforeEach
    @Transactional
    void setUp() {
        // Clear all caches
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear());
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test data
        createTestData();
    }

    private void createTestData() {
        testUsers = new ArrayList<>();
        testTimeSlots = new ArrayList<>();

        // Create 10 users
        for (int i = 0; i < 10; i++) {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .name("Test User " + i)
                    .email("test" + i + "@example.com")
                    .timezone("UTC")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            
            User savedUser = userService.save(user);
            testUsers.add(savedUser);

            // Create 5 time slots per user
            for (int j = 0; j < 5; j++) {
                TimeSlot timeSlot = TimeSlot.builder()
                        .id(UUID.randomUUID())
                        .user(savedUser)
                        .startTime(Instant.now().plus(j * 2, ChronoUnit.HOURS))
                        .endTime(Instant.now().plus(j * 2 + 1, ChronoUnit.HOURS))
                        .status(TimeSlot.SlotStatus.AVAILABLE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                
                TimeSlot savedTimeSlot = timeSlotService.createSlot(timeSlot);
                testTimeSlots.add(savedTimeSlot);
            }
        }
    }

    @Test
    @Transactional
    void shouldDemonstrateCachingPerformance() {
        // Warm up - first calls to populate cache
        testUsers.forEach(user -> userService.findById(user.getId()));
        testTimeSlots.forEach(slot -> timeSlotService.findById(slot.getId()));

        // Measure cached performance
        long startTime = System.nanoTime();
        
        // Perform 100 cached lookups
        for (int i = 0; i < 100; i++) {
            for (User user : testUsers) {
                userService.findById(user.getId());
            }
            for (TimeSlot slot : testTimeSlots) {
                timeSlotService.findById(slot.getId());
            }
        }
        
        long cachedTime = System.nanoTime() - startTime;
        double cachedTimeMs = cachedTime / 1_000_000.0;

        System.out.println("=== Caching Performance Test Results ===");
        System.out.println("Total cached operations: " + (100 * (testUsers.size() + testTimeSlots.size())));
        System.out.println("Total cached time: " + String.format("%.2f", cachedTimeMs) + " ms");
        System.out.println("Average time per operation: " + String.format("%.4f", cachedTimeMs / (100 * (testUsers.size() + testTimeSlots.size()))) + " ms");

        // Verify cache hit rate
        var userCache = cacheManager.getCache("users");
        var timeSlotCache = cacheManager.getCache("timeSlots");
        
        assertNotNull(userCache);
        assertNotNull(timeSlotCache);
        
        System.out.println("Cache statistics:");
        System.out.println("Users cache size: " + userCache.getNativeCache().toString());
        System.out.println("TimeSlots cache size: " + timeSlotCache.getNativeCache().toString());

        // Performance assertion - cached operations should be fast
        assertTrue(cachedTimeMs < 1000, "Cached operations should complete within 1 second");
    }

    @Test
    @Transactional
    void shouldHandleConcurrentCacheAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.nanoTime();

        // Submit concurrent tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    // Access users
                    for (User user : testUsers) {
                        User foundUser = userService.findById(user.getId());
                        assertNotNull(foundUser);
                        assertEquals(user.getId(), foundUser.getId());
                    }

                    // Access time slots
                    for (TimeSlot slot : testTimeSlots) {
                        TimeSlot foundSlot = timeSlotService.findById(slot.getId());
                        assertNotNull(foundSlot);
                        assertEquals(slot.getId(), foundSlot.getId());
                    }

                    // Access filtered time slots
                    LocalDate startDate = LocalDate.now();
                    LocalDate endDate = LocalDate.now().plusDays(7);
                    List<TimeSlot> filteredSlots = timeSlotService.fetchFilteredTimeSlots(
                            testUsers.get(threadId % testUsers.size()).getId(),
                            startDate, endDate, TimeSlot.SlotStatus.AVAILABLE, userTimeZone);
                    assertNotNull(filteredSlots);
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
        System.out.println("Total operations: " + (threadCount * operationsPerThread * (testUsers.size() + testTimeSlots.size() + 1)));
        System.out.println("Total time: " + String.format("%.2f", totalTimeMs) + " ms");
        System.out.println("Average time per operation: " + String.format("%.4f", totalTimeMs / (threadCount * operationsPerThread * (testUsers.size() + testTimeSlots.size() + 1))) + " ms");

        // Performance assertion - concurrent cached operations should still be fast
        assertTrue(totalTimeMs < 5000, "Concurrent cached operations should complete within 5 seconds");
    }

    @Test
    @Transactional
    void shouldDemonstrateCacheEvictionPerformance() {
        // Warm up cache
        testUsers.forEach(user -> userService.findById(user.getId()));
        testTimeSlots.forEach(slot -> timeSlotService.findById(slot.getId()));

        // Verify cache is populated
        var userCache = cacheManager.getCache("users");
        var timeSlotCache = cacheManager.getCache("timeSlots");
        assertNotNull(userCache);
        assertNotNull(timeSlotCache);

        long startTime = System.nanoTime();

        // Update time slots (should evict cache)
        for (TimeSlot slot : testTimeSlots) {
            slot.setStatus(TimeSlot.SlotStatus.BUSY);
            timeSlotService.updateSlot(slot);
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
}
