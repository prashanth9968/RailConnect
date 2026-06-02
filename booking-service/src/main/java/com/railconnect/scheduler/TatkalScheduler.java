package com.railconnect.scheduler;

import com.railconnect.repository.SeatLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class TatkalScheduler {

    private final SeatLockRepository seatLockRepository;

    /**
     * Cleans up expired seat locks every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanExpiredLocks() {
        int deleted = seatLockRepository.deleteExpiredLocks(Instant.now());
        if (deleted > 0) {
            log.info("Expired seat locks cleaned up: count={}", deleted);
        }
    }

    /**
     * Daily at 10:00 AM: Opens Tatkal reservations for AC classes.
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void openAcTatkalBookings() {
        log.info("SYSTEM ALERTS: Tatkal AC bookings are now OPEN for today!");
    }

    /**
     * Daily at 11:00 AM: Opens Tatkal reservations for Non-AC classes.
     */
    @Scheduled(cron = "0 0 11 * * ?")
    public void openNonAcTatkalBookings() {
        log.info("SYSTEM ALERTS: Tatkal Non-AC bookings are now OPEN for today!");
    }
}
