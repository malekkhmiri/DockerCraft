package com.user.userservice;

import com.user.userservice.security.JwtService;
import com.user.userservice.dto.*;
import com.user.userservice.verification.StudentVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ActivityService activityService;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private StudentVerificationRepository studentVerificationRepository;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");
        user = User.builder()
                .id(1L)
                .displayName("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.USER)
                .emailVerified(true) // Simuler un utilisateur vérifié pour le login
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByDisplayName(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        AuthResponse response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getMessage().contains("code de vérification"));
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationCode(anyString(), anyString());
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("mockToken");
        when(studentVerificationRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        AuthResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("mockToken", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_NotVerified_ThrowsException() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.login(loginRequest));
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));

        assertThrows(BadCredentialsException.class, () -> userService.login(loginRequest));
    }
}
