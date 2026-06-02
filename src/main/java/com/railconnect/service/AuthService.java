package com.railconnect.service;

import com.railconnect.dto.request.*;
import com.railconnect.dto.response.*;
import com.railconnect.entity.User;
import com.railconnect.enums.UserRole;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.UserRepository;
import com.railconnect.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RailConnectException("Email already registered", HttpStatus.CONFLICT);
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RailConnectException("Phone number already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(UserRole.ROLE_USER)
            .build();

        userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(userDetails))
            .refreshToken(jwtService.generateRefreshToken(userDetails))
            .expiresIn(86400)
            .user(toUserResponse(user))
            .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RailConnectException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.isAccountNonLocked()) {
            if (user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                throw new RailConnectException("Account locked due to too many failed attempts. Try after 30 minutes.", HttpStatus.LOCKED);
            } else {
                userRepository.resetFailedAttempts(user.getEmail());
            }
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
                userRepository.save(user);
                throw new RailConnectException("Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts.", HttpStatus.LOCKED);
            }
            userRepository.incrementFailedAttempts(user.getEmail());
            throw new RailConnectException("Invalid email or password. " + (MAX_FAILED_ATTEMPTS - attempts) + " attempts remaining.", HttpStatus.UNAUTHORIZED);
        }

        userRepository.resetFailedAttempts(user.getEmail());
        CustomUserDetails userDetails = new CustomUserDetails(user);

        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(userDetails))
            .refreshToken(jwtService.generateRefreshToken(userDetails))
            .expiresIn(86400)
            .user(toUserResponse(user))
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RailConnectException("Invalid token", HttpStatus.UNAUTHORIZED));
        CustomUserDetails userDetails = new CustomUserDetails(user);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RailConnectException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }
        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(userDetails))
            .refreshToken(refreshToken)
            .expiresIn(86400)
            .user(toUserResponse(user))
            .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .profilePicture(user.getProfilePicture())
            .role(user.getRole().name())
            .build();
    }
}
