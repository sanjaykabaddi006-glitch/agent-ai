package com.knowledgehub.service;

import com.knowledgehub.common.exception.BadRequestException;
import com.knowledgehub.common.exception.ResourceNotFoundException;
import com.knowledgehub.config.JwtTokenProvider;
import com.knowledgehub.domain.user.Role;
import com.knowledgehub.domain.user.User;
import com.knowledgehub.domain.user.UserRepository;
import com.knowledgehub.dto.auth.AuthResponse;
import com.knowledgehub.dto.auth.LoginRequest;
import com.knowledgehub.dto.auth.RegisterRequest;
import com.knowledgehub.dto.auth.UserProfileResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new BadRequestException("User account is disabled");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        auditService.log(user.getId(), "LOGIN", "USER", user.getId(), "User logged in successfully", ipAddress);

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.ROLE_USER
        );

        User saved = userRepository.save(user);
        auditService.log(saved.getId(), "REGISTER", "USER", saved.getId(), "User registered", ipAddress);

        String token = tokenProvider.generateToken(saved.getId(), saved.getUsername(), saved.getRole().name());

        return new AuthResponse(
                token,
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole().name()
        );
    }

    public UserProfileResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
