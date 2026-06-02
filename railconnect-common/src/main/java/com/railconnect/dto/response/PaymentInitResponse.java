package com.railconnect.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class PaymentInitResponse {
    private String razorpayOrderId;
    private String razorpayKeyId;
    private BigDecimal amount;
    private String currency;
    private String pnrNumber;
    // UPI intent URL for GPay/PhonePe
    private String upiIntentUrl;
    private String qrCodeData;
}
