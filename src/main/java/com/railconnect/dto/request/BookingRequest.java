package com.railconnect.dto.request;

import com.railconnect.enums.QuotaType;
import com.railconnect.enums.SeatClass;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull private Long trainId;
    @NotBlank private String sourceStationCode;
    @NotBlank private String destinationStationCode;
    @NotNull @Future private LocalDate journeyDate;
    @NotNull private SeatClass seatClass;
    private QuotaType quotaType = QuotaType.GENERAL;
    @NotEmpty @Size(max = 6) private List<PassengerRequest> passengers;
    private String boardingStationCode;
    private boolean smsAlert = true;
    private boolean emailAlert = true;
}
