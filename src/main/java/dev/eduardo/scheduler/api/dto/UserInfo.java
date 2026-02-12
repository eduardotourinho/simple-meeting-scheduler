package dev.eduardo.scheduler.api.dto;

import dev.eduardo.scheduler.domain.entities.User;

public record UserInfo(
        String name,
        String email
) {
    public static UserInfo fromEntity(User user) {
        return new UserInfo(user.getName(), user.getEmail());
    }
}
