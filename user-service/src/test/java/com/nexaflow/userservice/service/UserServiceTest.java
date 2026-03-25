package com.nexaflow.userservice.service;

import com.nexaflow.userservice.dto.*;
import com.nexaflow.userservice.model.User;
import com.nexaflow.userservice.model.UserRole;
import com.nexaflow.userservice.repository.UserRepository;
import com.nexaflow.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("pass123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setRole(UserRole.CUSTOMER);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("token123");

        LoginResponse response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        verify(kafkaTemplate).send(eq("user-events"), anyString(), any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyLong(), anyString())).thenReturn("token123");

        LoginResponse response = userService.login(req);

        assertNotNull(response);
        assertEquals("token123", response.getToken());
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.login(req));
    }
}
