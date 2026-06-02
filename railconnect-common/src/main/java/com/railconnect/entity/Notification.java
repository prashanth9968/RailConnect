package com.railconnect.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String message;
    @Column(name = "notification_type") private String notificationType;
    @Column(name = "reference_id") private UUID referenceId;
    @Builder.Default @Column(name = "is_read") private boolean read = false;
    @Builder.Default @Column(name = "sent_via_email") private boolean sentViaEmail = false;
    @Builder.Default @Column(name = "sent_via_sms") private boolean sentViaSms = false;
}
