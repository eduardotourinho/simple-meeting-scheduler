package dev.eduardo.scheduler.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CreateUserRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Timezone is required")
        @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$",
                message = "Invalid timezone format. Use format: Area/Location (e.g., Europe/Berlin)")
        String timezone
) {}