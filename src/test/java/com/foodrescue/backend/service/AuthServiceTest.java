package com.foodrescue.backend.service;

import com.foodrescue.backend.dto.LoginRequest;
import com.foodrescue.backend.dto.RegisterRequest;
import com.foodrescue.backend.dto.AuthResponse;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.enums.Role;
import com.foodrescue.backend.enums.UserStatus;
import com.foodrescue.backend.repository.UserRepository;
import com.foodrescue.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret123");
        request.setPhone("9876543210");
        request.setRole(Role.DONOR);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken("alice@example.com", "DONOR")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("DONOR");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setRole(Role.DONOR);
        request.setName("Bob");
        request.setPassword("pass123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        User user = new User("Alice", "alice@example.com",
                passwordEncoder.encode("secret123"), "9876543210", Role.DONOR);
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alice@example.com", "DONOR")).thenReturn("mock-jwt-token");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("secret123");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getRole()).isEqualTo("DONOR");
    }

    @Test
    void login_shouldThrowForWrongPassword() {
        User user = new User("Alice", "alice@example.com",
                passwordEncoder.encode("correctPassword"), "9876543210", Role.DONOR);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("wrongPassword");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }
}
