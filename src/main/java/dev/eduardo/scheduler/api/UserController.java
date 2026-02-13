package dev.eduardo.scheduler.api;

import dev.eduardo.scheduler.service.UserApiService;
import dev.eduardo.scheduler.api.dto.CreateUserRequest;
import dev.eduardo.scheduler.api.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserApiService userService;

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user account with the provided details")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.email());
        var user = userService.createUser(request);
        log.info("Successfully created user with ID: {}", user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}