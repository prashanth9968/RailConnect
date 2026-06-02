package com.railconnect.service.impl;
import com.railconnect.dto.request.LoginRequest;
import com.railconnect.dto.request.RegisterRequest;
import com.railconnect.dto.response.AuthResponse;
import com.railconnect.dto.response.UserResponse;
import com.railconnect.entity.User;
import com.railconnect.enums.AuthProvider;
import com.railconnect.enums.Role;
import com.railconnect.exception.BusinessException;
import com.railconnect.repository.UserRepository;
import com.railconnect.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
@Service @Slf4j @RequiredArgsConstructor
public class AuthServiceImpl {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BusinessException("Email already registered", "EMAIL_EXISTS");
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone()))
            throw new BusinessException("Phone already registered", "PHONE_EXISTS");
        User user = User.builder()
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .firstName(req.getFirstName())
            .lastName(req.getLastName())
            .phone(req.getPhone())
            .dateOfBirth(req.getDateOfBirth() != null ? LocalDate.parse(req.getDateOfBirth()) : null)
            .gender(req.getGender())
            .role(Role.PASSENGER)
            .authProvider(AuthProvider.LOCAL)
            .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new BusinessException("Invalid credentials", "INVALID_CREDENTIALS"));
        if (user.isAccountLocked())
            throw new BusinessException("Account is locked. Contact support.", "ACCOUNT_LOCKED");
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            userRepository.incrementFailedLoginAttempts(user.getId());
            if (user.getFailedLoginAttempts() + 1 >= 5) {
                user.setAccountLocked(true);
                userRepository.save(user);
                throw new BusinessException("Account locked after 5 failed attempts", "ACCOUNT_LOCKED");
            }
            throw new BusinessException("Invalid credentials", "INVALID_CREDENTIALS");
        }
        userRepository.resetLoginAttempts(user.getId());
        user.setLastLogin(Instant.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        UserResponse userResponse = UserResponse.builder()
            .id(user.getId()).email(user.getEmail())
            .firstName(user.getFirstName()).lastName(user.getLastName())
            .phone(user.getPhone()).role(user.getRole().name())
            .profilePictureUrl(user.getProfilePictureUrl())
            .emailVerified(user.isEmailVerified()).phoneVerified(user.isPhoneVerified())
            .build();
        return AuthResponse.builder()
            .accessToken(accessToken).refreshToken(refreshToken)
            .tokenType("Bearer").expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
            .user(userResponse).build();
    }
}
