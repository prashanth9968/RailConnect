package com.railconnect.dto.response;

import com.railconnect.enums.PaymentMethod;
import com.railconnect.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String gatewayOrderId;
    private String gatewayReferenceId;
    private LocalDateTime completedAt;
    private BigDecimal refundAmount;
}
