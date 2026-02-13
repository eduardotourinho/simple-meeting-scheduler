package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#email.toLowerCase()")
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Cacheable(value = "users", key = "#userId")
    public User findById(UUID userId) {
        log.debug("Finding user by ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    public User save(User user) {
        log.debug("Saving user: {}", user.getId());
        return userRepository.save(user);
    }
}
