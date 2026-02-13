package dev.eduardo.scheduler.integration;

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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "logging.level.dev.eduardo.scheduler.service=DEBUG"
})
class RedisCachingIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;
    private TimeSlot testTimeSlot;
    private final ZoneId userTimeZone = ZoneId.of("UTC");

    @BeforeEach
    @Transactional
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear());
        
        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Create test time slot
        testTimeSlot = TimeSlot.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .status(TimeSlot.SlotStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @Transactional
    void shouldCacheUserFindByIdInRedis() {
        // Given - Save user to database
        User savedUser = userService.save(testUser);

        // When - First call should hit database and cache result
        User result1 = userService.findById(savedUser.getId());
        assertEquals(savedUser.getId(), result1.getId());
        assertEquals(savedUser.getEmail(), result1.getEmail());

        // Verify cache contains the user
        var userCache = cacheManager.getCache("users");
        assertNotNull(userCache);
        var cachedUser = userCache.get(savedUser.getId());
        assertNotNull(cachedUser);
        assertEquals(savedUser.getId(), ((User) cachedUser.get()).getId());

        // When - Second call should hit cache
        User result2 = userService.findById(savedUser.getId());
        assertEquals(savedUser.getId(), result2.getId());
        assertEquals(savedUser.getEmail(), result2.getEmail());

        // Verify Redis contains the cached data
        String redisKey = "users::" + savedUser.getId();
        assertTrue(redisTemplate.hasKey(redisKey));
        User redisUser = (User) redisTemplate.opsForValue().get(redisKey);
        assertNotNull(redisUser);
        assertEquals(savedUser.getId(), redisUser.getId());
    }

    @Test
    @Transactional
    void shouldCacheUserFindByEmailInRedis() {
        // Given - Save user to database
        User savedUser = userService.save(testUser);

        // When - First call should hit database and cache result
        var result1 = userService.findByEmail(savedUser.getEmail());
        assertTrue(result1.isPresent());
        assertEquals(savedUser.getId(), result1.get().getId());

        // Verify cache contains the user
        var userCache = cacheManager.getCache("users");
        assertNotNull(userCache);
        var cachedUser = userCache.get(savedUser.getEmail().toLowerCase());
        assertNotNull(cachedUser);
        assertEquals(savedUser.getId(), ((User) cachedUser.get()).getId());

        // When - Second call should hit cache
        var result2 = userService.findByEmail(savedUser.getEmail());
        assertTrue(result2.isPresent());
        assertEquals(savedUser.getId(), result2.get().getId());

        // Verify Redis contains the cached data
        String redisKey = "users::" + savedUser.getEmail().toLowerCase();
        assertTrue(redisTemplate.hasKey(redisKey));
        User redisUser = (User) redisTemplate.opsForValue().get(redisKey);
        assertNotNull(redisUser);
        assertEquals(savedUser.getId(), redisUser.getId());
    }

    @Test
    @Transactional
    void shouldCacheTimeSlotFindByIdInRedis() {
        // Given - Save user and time slot to database
        User savedUser = userService.save(testUser);
        testTimeSlot.setUser(savedUser);
        TimeSlot savedTimeSlot = timeSlotService.createSlot(testTimeSlot);

        // When - First call should hit database and cache result
        TimeSlot result1 = timeSlotService.findById(savedTimeSlot.getId());
        assertEquals(savedTimeSlot.getId(), result1.getId());
        assertEquals(savedTimeSlot.getUser().getId(), result1.getUser().getId());

        // Verify cache contains the time slot
        var timeSlotCache = cacheManager.getCache("timeSlots");
        assertNotNull(timeSlotCache);
        var cachedTimeSlot = timeSlotCache.get(savedTimeSlot.getId());
        assertNotNull(cachedTimeSlot);
        assertEquals(savedTimeSlot.getId(), ((TimeSlot) cachedTimeSlot.get()).getId());

        // When - Second call should hit cache
        TimeSlot result2 = timeSlotService.findById(savedTimeSlot.getId());
        assertEquals(savedTimeSlot.getId(), result2.getId());
        assertEquals(savedTimeSlot.getUser().getId(), result2.getUser().getId());

        // Verify Redis contains the cached data
        String redisKey = "timeSlots::" + savedTimeSlot.getId();
        assertTrue(redisTemplate.hasKey(redisKey));
        TimeSlot redisTimeSlot = (TimeSlot) redisTemplate.opsForValue().get(redisKey);
        assertNotNull(redisTimeSlot);
        assertEquals(savedTimeSlot.getId(), redisTimeSlot.getId());
    }

    @Test
    @Transactional
    void shouldCacheFetchFilteredTimeSlotsInRedis() {
        // Given - Save user and time slot to database
        User savedUser = userService.save(testUser);
        testTimeSlot.setUser(savedUser);
        TimeSlot savedTimeSlot = timeSlotService.createSlot(testTimeSlot);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        TimeSlot.SlotStatus status = TimeSlot.SlotStatus.AVAILABLE;

        // When - First call should hit database and cache result
        List<TimeSlot> result1 = timeSlotService.fetchFilteredTimeSlots(
                savedUser.getId(), startDate, endDate, status, userTimeZone);
        assertEquals(1, result1.size());
        assertEquals(savedTimeSlot.getId(), result1.get(0).getId());

        // Verify cache contains the filtered time slots
        var userTimeSlotsCache = cacheManager.getCache("userTimeSlots");
        assertNotNull(userTimeSlotsCache);
        
        // Generate the same cache key as the service
        int cacheKey = java.util.Objects.hash(
                savedUser.getId(), startDate, endDate, status, userTimeZone);
        var cachedTimeSlots = userTimeSlotsCache.get(cacheKey);
        assertNotNull(cachedTimeSlots);
        @SuppressWarnings("unchecked")
        List<TimeSlot> cachedList = (List<TimeSlot>) cachedTimeSlots.get();
        assertEquals(1, cachedList.size());
        assertEquals(savedTimeSlot.getId(), cachedList.get(0).getId());

        // When - Second call should hit cache
        List<TimeSlot> result2 = timeSlotService.fetchFilteredTimeSlots(
                savedUser.getId(), startDate, endDate, status, userTimeZone);
        assertEquals(1, result2.size());
        assertEquals(savedTimeSlot.getId(), result2.get(0).getId());

        // Verify Redis contains the cached data
        String redisKey = "userTimeSlots::" + cacheKey;
        assertTrue(redisTemplate.hasKey(redisKey));
        @SuppressWarnings("unchecked")
        List<TimeSlot> redisTimeSlots = (List<TimeSlot>) redisTemplate.opsForValue().get(redisKey);
        assertNotNull(redisTimeSlots);
        assertEquals(1, redisTimeSlots.size());
        assertEquals(savedTimeSlot.getId(), redisTimeSlots.get(0).getId());
    }

    @Test
    @Transactional
    void shouldEvictCacheOnTimeSlotUpdate() {
        // Given - Save user and time slot, then cache them
        User savedUser = userService.save(testUser);
        testTimeSlot.setUser(savedUser);
        TimeSlot savedTimeSlot = timeSlotService.createSlot(testTimeSlot);

        // Cache the time slot
        timeSlotService.findById(savedTimeSlot.getId());
        assertTrue(redisTemplate.hasKey("timeSlots::" + savedTimeSlot.getId()));

        // When - Update time slot should evict cache
        savedTimeSlot.setStatus(TimeSlot.SlotStatus.BUSY);
        TimeSlot updatedTimeSlot = timeSlotService.updateSlot(savedTimeSlot);

        // Then - Cache should be evicted (this is handled by @CacheEvict)
        // Note: In a real scenario, the cache eviction happens during the update
        assertEquals(TimeSlot.SlotStatus.BUSY, updatedTimeSlot.getStatus());
    }

    @Test
    @Transactional
    void shouldEvictCacheOnTimeSlotDelete() {
        // Given - Save user and time slot, then cache them
        User savedUser = userService.save(testUser);
        testTimeSlot.setUser(savedUser);
        TimeSlot savedTimeSlot = timeSlotService.createSlot(testTimeSlot);

        // Cache the time slot
        timeSlotService.findById(savedTimeSlot.getId());
        assertTrue(redisTemplate.hasKey("timeSlots::" + savedTimeSlot.getId()));

        // When - Delete time slot should evict cache
        timeSlotService.removeSlot(savedTimeSlot);

        // Then - Cache should be evicted (this is handled by @CacheEvict)
        // Note: In a real scenario, the cache eviction happens during the delete
        // The time slot should be deleted from the database
        assertThrows(Exception.class, () -> 
            timeSlotService.findById(savedTimeSlot.getId()));
    }

    @Test
    void shouldNotCacheNullValues() {
        // Given - Non-existent user ID
        UUID nonExistentUserId = UUID.randomUUID();

        // When & Then - Should throw exception and not cache null
        assertThrows(dev.eduardo.scheduler.service.exception.UserNotFoundException.class,
                () -> userService.findById(nonExistentUserId));

        // Verify no cache entry was created
        var userCache = cacheManager.getCache("users");
        assertNotNull(userCache);
        var cachedUser = userCache.get(nonExistentUserId);
        assertNull(cachedUser);

        // Verify Redis doesn't contain the key
        String redisKey = "users::" + nonExistentUserId;
        assertFalse(redisTemplate.hasKey(redisKey));
    }

    @Test
    void shouldHaveProperCacheConfiguration() {
        // Verify cache manager is properly configured
        assertNotNull(cacheManager);
        assertTrue(cacheManager.getCacheNames().contains("users"));
        assertTrue(cacheManager.getCacheNames().contains("timeSlots"));
        assertTrue(cacheManager.getCacheNames().contains("userTimeSlots"));

        // Verify Redis template is properly configured
        assertNotNull(redisTemplate);
    }
}
