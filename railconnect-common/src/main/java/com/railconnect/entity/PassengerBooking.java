package com.railconnect.entity;

import com.railconnect.enums.BookingStatus;
import com.railconnect.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "passenger_bookings", indexes = {
    @Index(name = "idx_pb_booking", columnList = "booking_id"),
    @Index(name = "idx_pb_seat", columnList = "seat_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PassengerBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "id_proof_type", length = 30)
    private String idProofType;  // AADHAAR, PAN, PASSPORT

    @Column(name = "id_proof_number", length = 30)
    private String idProofNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus seatStatus = BookingStatus.INITIATED;

    @Column(name = "berth_preference", length = 20)
    private String berthPreference;

    @Column(name = "is_senior_citizen")
    @Builder.Default
    private boolean seniorCitizen = false;

    @Column(name = "meal_preference", length = 20)
    private String mealPreference;  // VEG, NON_VEG, JAIN
}
