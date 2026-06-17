package com.railconnect.repository;
import com.railconnect.entity.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SeatLockRepository extends JpaRepository<SeatLock, UUID> {
    Optional<SeatLock> findByLockToken(String lockToken);
    List<SeatLock> findByUserIdAndExpiresAtAfter(UUID userId, Instant now);
    @Modifying
    @Query("DELETE FROM SeatLock sl WHERE sl.expiresAt < :now")
    int deleteExpiredLocks(@Param("now") Instant now);
    boolean existsByScheduleIdAndClassTypeAndSeatNumberAndCoachNumberAndExpiresAtAfter(
        Long scheduleId, String classType, String seatNumber, String coachNumber, Instant now);
}
