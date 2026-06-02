package com.railconnect.service.impl;

import com.railconnect.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockSchedulerService {
    private final SeatRepository seatRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredSeatLocks() {
        int released = seatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) log.info("Released {} expired seat locks", released);
    }
}
