package com.railconnect.exception;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
    }
    public BusinessException(String message) { this(message, "BUSINESS_ERROR"); }
}
