package com.knowledgehub;

import com.knowledgehub.config.JwtTokenProvider;
import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.user.Role;
import com.knowledgehub.domain.user.User;
import com.knowledgehub.domain.user.UserRepository;
import com.knowledgehub.dto.auth.AuthResponse;
import com.knowledgehub.dto.auth.LoginRequest;
import com.knowledgehub.dto.auth.RegisterRequest;
import com.knowledgehub.service.AuditService;
import com.knowledgehub.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuditService auditService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();

        KnowledgeHubProperties properties = new KnowledgeHubProperties();
        tokenProvider = new JwtTokenProvider(properties);
        auditService = Mockito.mock(AuditService.class);

        authService = new AuthService(userRepository, passwordEncoder, tokenProvider, auditService);
    }

    @Test
    @DisplayName("Should register new user and return JWT token")
    void testRegisterUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@company.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest("newuser", "new@company.com", "SecurePass123!", "Jane Doe");
        AuthResponse response = authService.register(request, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("newuser", response.getUsername());
        assertEquals("ROLE_USER", response.getRole());
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void testLoginSuccess() {
        String hashed = passwordEncoder.encode("Secret123!");
        User user = new User("alice", "alice@company.com", hashed, "Alice Smith", Role.ROLE_USER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("alice", "Secret123!");
        AuthResponse response = authService.login(request, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("alice", response.getUsername());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid password")
    void testLoginInvalidPassword() {
        String hashed = passwordEncoder.encode("Secret123!");
        User user = new User("bob", "bob@company.com", hashed, "Bob Smith", Role.ROLE_USER);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("bob", "WrongPassword!");
        assertThrows(BadCredentialsException.class, () -> authService.login(request, "127.0.0.1"));
    }
}
