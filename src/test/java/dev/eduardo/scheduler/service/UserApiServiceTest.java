package dev.eduardo.scheduler.service;

import dev.eduardo.scheduler.api.dto.CreateUserRequest;
import dev.eduardo.scheduler.domain.entities.User;
import dev.eduardo.scheduler.domain.repository.UserRepository;
import dev.eduardo.scheduler.service.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApiServiceTest {

    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private UserApiService userApiService;

    public static Stream<Arguments> createUserRequestProvider() {
        return Stream.of(
                Arguments.of(CreateUserRequest.builder()
                        .name("John Doe")
                        .email("john.doe@example.com")
                        .timezone("America/New_York")
                        .build()),
                Arguments.of(CreateUserRequest.builder()
                        .name("Test")
                        .email("test@test.com")
                        .timezone("Europe/Berlin")
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("createUserRequestProvider")
    void shouldValidateNewUserRequest(CreateUserRequest createUserRequest) {

        when(userRepositoryMock.save(any(User.class)))
                .thenReturn(User.builder().build());

        assertDoesNotThrow(() -> userApiService.createUser(createUserRequest));

        verify(userRepositoryMock, times(1))
                .save(any(User.class));
    }

    @Test
    void shouldFailWhenUserEmailAlreadyExists() {
        var email = "test@example.com";

        var createUserRequest = CreateUserRequest.builder()
                .name("Test")
                .email(email)
                .timezone("Europe/Berlin")
                .build();

        when(userRepositoryMock.existsByEmail(email))
                .thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> userApiService.createUser(createUserRequest));

        verify(userRepositoryMock, never())
                .save(any(User.class));

    }

    @ParameterizedTest
    @ValueSource(strings = {"Europe/Berlim", "Invalid/Timezone"})
    void shouldFailWhenUserTimezoneDontExist(String timezone) {
        var email = "test@example.com";

        var createUserRequest = CreateUserRequest.builder()
                .name("Test")
                .email(email)
                .timezone(timezone)
                .build();

        when(userRepositoryMock.existsByEmail(email))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userApiService.createUser(createUserRequest));

        verify(userRepositoryMock, never())
                .save(any(User.class));

    }
}