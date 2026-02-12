package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";

    @Test
    void shouldFindUserByEmail() {
        // Given
        var user = createTestUser();
        when(userRepositoryMock.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());
        assertEquals(user.getEmail(), result.get().getEmail());

        verify(userRepositoryMock).findByEmail(email.toLowerCase());
    }

    @Test
    void shouldReturnEmpty_WhenEmailNotFound() {
        // Given
        when(userRepositoryMock.findByEmail(email)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertFalse(result.isPresent());

        verify(userRepositoryMock).findByEmail(email.toLowerCase());
    }

    @Test
    void shouldFindUserById() {
        // Given
        var user = createTestUser();
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));

        // When
        User result = userService.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepositoryMock).findById(userId);
    }

    @Test
    void shouldThrowUserNotFoundException_WhenUserNotFound() {
        // Given
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, 
                () -> userService.findById(userId));

        verify(userRepositoryMock).findById(userId);
    }

    @Test
    void shouldConvertEmailToLowercase() {
        // Given
        var uppercaseEmail = "TEST@EXAMPLE.COM";
        var user = createTestUser();
        when(userRepositoryMock.findByEmail(uppercaseEmail.toLowerCase())).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findByEmail(uppercaseEmail);

        // Then
        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());

        verify(userRepositoryMock).findByEmail("test@example.com");
    }

    private User createTestUser() {
        return User.builder()
                .id(userId)
                .name("Test User")
                .email(email)
                .timezone("UTC")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
