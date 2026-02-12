package dev.eduardo.scheduler.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateMeetingRequest(
        @NotBlank(message = "Meeting title is required")
        String title,
        
        String description,
        
        @NotEmpty(message = "At least one participant is required")
        @Valid
        List<ParticipantRequest> participants
) {

    @Builder
    public record ParticipantRequest(
            @NotBlank(message = "Participant name is required")
            String name,
            
            @NotBlank(message = "Participant email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}
}
