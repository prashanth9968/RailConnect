package com.railconnect.service.impl;

import com.railconnect.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockServiceImpl implements SeatLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${railconnect.seat-lock.timeout-minutes}")
    private int lockTimeoutMinutes;

    private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";
    private static final String BOOKING_SEATS_KEY_PREFIX = "booking:seats:";

    @Override
    public void lockSeats(List<Long> seatIds, String userId, String sessionId) {
        String lockValue = userId + ":" + sessionId;
        for (Long seatId : seatIds) {
            String key = SEAT_LOCK_KEY_PREFIX + seatId;
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockValue, Duration.ofMinutes(lockTimeoutMinutes));
            if (Boolean.FALSE.equals(locked)) {
                // Rollback already locked seats
                seatIds.subList(0, seatIds.indexOf(seatId))
                        .forEach(id -> redisTemplate.delete(SEAT_LOCK_KEY_PREFIX + id));
                throw new RuntimeException("Seat " + seatId + " was just taken by another user. Please retry.");
            }
            log.debug("Seat locked: seatId={}, userId={}", seatId, userId);
        }
        // Store mapping of sessionId -> seatIds for bulk release
        redisTemplate.opsForSet().add(BOOKING_SEATS_KEY_PREFIX + sessionId,
                seatIds.stream().map(String::valueOf).toArray());
        redisTemplate.expire(BOOKING_SEATS_KEY_PREFIX + sessionId, Duration.ofMinutes(lockTimeoutMinutes + 5));
    }

    @Override
    public void releaseLocks(String bookingId) {
        String bookingKey = BOOKING_SEATS_KEY_PREFIX + bookingId;
        var members = redisTemplate.opsForSet().members(bookingKey);
        if (members != null) {
            members.forEach(m -> redisTemplate.delete(SEAT_LOCK_KEY_PREFIX + m));
            redisTemplate.delete(bookingKey);
            log.debug("Released seat locks for booking: {}", bookingId);
        }
    }

    @Override
    public boolean isSeatLocked(Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SEAT_LOCK_KEY_PREFIX + seatId));
    }
}
