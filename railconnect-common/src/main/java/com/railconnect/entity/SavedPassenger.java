package com.railconnect.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "saved_passengers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SavedPassenger extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private int age;
    @Column(nullable = false) private String gender;
    @Column(name = "id_type") private String idType;
    @Column(name = "id_number") private String idNumber;
    @Builder.Default @Column(name = "is_senior_citizen") private boolean seniorCitizen = false;
}
