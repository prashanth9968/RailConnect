package com.railconnect.service;

import java.util.List;

public interface SeatLockService {
    void lockSeats(List<Long> seatIds, String userId, String sessionId);
    void releaseLocks(String bookingId);
    boolean isSeatLocked(Long seatId);
}
