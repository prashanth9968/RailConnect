package com.railconnect.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class PassengerResponse {
    private UUID id;
    private String name;
    private int age;
    private String gender;
    private String seatNumber;
    private String coachNumber;
    private String berthType;
    private String bookingStatus;
    private int waitlistNumber;
}
