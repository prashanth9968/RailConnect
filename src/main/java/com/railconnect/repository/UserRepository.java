package com.railconnect.repository;

import com.railconnect.entity.User;
import com.railconnect.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByProviderIdAndProvider(String providerId, AuthProvider provider);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountNonLocked = true, u.lockTime = null WHERE u.email = :email")
    void resetFailedAttempts(String email);
}
