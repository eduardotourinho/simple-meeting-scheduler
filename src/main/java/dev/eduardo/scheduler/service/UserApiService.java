package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.CreateUserRequest;
import dev.eduardo.scheduler.api.dto.UserResponse;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.DuplicateEmailException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(@Valid CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already exist: " + request.email());
        }

        // Validate timezone
        if (!isValidTimeZone(request.timezone())) {
            log.warn("Invalid timezone: {}", request.timezone());
            throw new IllegalArgumentException("Invalid timezone: " + request.timezone());
        }

        // Create and save user
        var user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .timezone(request.timezone())
                .build();

        var savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    private boolean isValidTimeZone(String timeZoneId) {
        try {
            var zoneId = ZoneId.of(timeZoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}