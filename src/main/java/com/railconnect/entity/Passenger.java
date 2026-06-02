package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "passengers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Passenger {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private String name;
    private int age;
    private String gender;
    private String nationality = "Indian";
    private String idType;   // AADHAAR, PAN, PASSPORT
    private String idNumber;

    private String seatNumber;
    private String coachNumber;
    private String berthPreference;

    private String bookingStatus; // CONFIRMED, RAC, WL/1, WL/2 ...
    private int waitlistNumber;
}
