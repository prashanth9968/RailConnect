package com.railconnect.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder
public class PaymentOrderResponse {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String gatewayKeyId;
    private String bookingId;
    private String pnr;
    private String gatewayName;
    private String upiDeepLink;
}
