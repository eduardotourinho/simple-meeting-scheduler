package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String timezone,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTimezone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}