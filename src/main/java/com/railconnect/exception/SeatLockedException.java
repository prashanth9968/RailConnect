package com.railconnect.exception;

import org.springframework.http.HttpStatus;

public class SeatLockedException extends RailConnectException {
    public SeatLockedException() {
        super("Seats are locked by another transaction. Please try again.", HttpStatus.CONFLICT);
    }
}
