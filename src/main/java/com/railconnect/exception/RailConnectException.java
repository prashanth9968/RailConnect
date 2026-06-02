package com.railconnect.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class RailConnectException extends RuntimeException {
    private final HttpStatus status;
    public RailConnectException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
