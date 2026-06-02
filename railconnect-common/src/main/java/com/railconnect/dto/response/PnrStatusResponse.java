package com.railconnect.dto.response;

import com.railconnect.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data @Builder
public class PnrStatusResponse {
    private String pnrNumber;
    private String trainNumber;
    private String trainName;
    private LocalDate journeyDate;
    private String fromStation;
    private String toStation;
    private String seatClass;
    private BookingStatus chartStatus;
    private List<PassengerResponse> passengers;
    private String boardingPoint;
}
