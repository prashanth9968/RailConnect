package com.railconnect.dto.response;

import com.railconnect.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class BookingResponse {
    private UUID id;
    private String pnrNumber;
    private String trainNumber;
    private String trainName;
    private LocalDate journeyDate;
    private String sourceStation;
    private String destinationStation;
    private SeatClass seatClass;
    private QuotaType quotaType;
    private BookingStatus status;
    private List<PassengerResponse> passengers;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime bookedAt;
}
