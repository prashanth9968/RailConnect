package com.railconnect.dto.request;

import com.railconnect.enums.QuotaType;
import com.railconnect.enums.SeatClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TrainSearchRequest {
    @NotBlank private String fromStation;
    @NotBlank private String toStation;
    @NotNull @FutureOrPresent private LocalDate journeyDate;
    private SeatClass seatClass;
    private QuotaType quotaType = QuotaType.GENERAL;
    private int passengerCount = 1;
    private boolean flexibleDate = false;
}
