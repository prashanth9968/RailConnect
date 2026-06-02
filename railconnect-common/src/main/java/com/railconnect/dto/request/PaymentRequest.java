package com.railconnect.dto.request;

import com.railconnect.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull private UUID bookingId;
    @NotNull private PaymentMethod paymentMethod;
    private String upiVpa;  // for GPay/PhonePe UPI ID
}
