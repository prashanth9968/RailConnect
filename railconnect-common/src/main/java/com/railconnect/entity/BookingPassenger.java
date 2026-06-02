package com.railconnect.entity;
import com.railconnect.enums.PassengerStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "booking_passengers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingPassenger extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @Column(name = "passenger_name", nullable = false) private String passengerName;
    @Column(nullable = false) private int age;
    @Column(nullable = false) private String gender;
    @Column(name = "id_type") private String idType;
    @Column(name = "id_number") private String idNumber;
    @Column(name = "berth_preference") private String berthPreference;
    @Builder.Default @Column(name = "is_senior_citizen") private boolean seniorCitizen = false;
    @Column(name = "seat_number") private String seatNumber;
    @Column(name = "coach_number") private String coachNumber;
    @Column(name = "berth_type") private String berthType;
    @Enumerated(EnumType.STRING) @Builder.Default private PassengerStatus status = PassengerStatus.CNF;
    @Column(name = "waitlist_number") private Integer waitlistNumber;
    @Column(nullable = false) private BigDecimal fare;
    @Column(name = "concession_type") private String concessionType;
    @Builder.Default @Column(name = "concession_amount") private BigDecimal concessionAmount = BigDecimal.ZERO;
}
