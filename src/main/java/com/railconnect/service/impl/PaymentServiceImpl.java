package com.railconnect.service.impl;
import com.railconnect.dto.request.PaymentRequest;
import com.railconnect.dto.response.PaymentOrderResponse;
import com.railconnect.entity.*;
import com.railconnect.enums.*;
import com.railconnect.exception.*;
import com.railconnect.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
@Service @Slf4j @RequiredArgsConstructor
public class PaymentServiceImpl {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationServiceImpl notificationService;
    @Value("${payment.razorpay.key-id}") private String razorpayKeyId;
    @Value("${payment.razorpay.key-secret}") private String razorpayKeySecret;
    @Value("${payment.razorpay.webhook-secret}") private String webhookSecret;

    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentRequest req, UUID userId) {
        Booking booking = bookingRepository.findById(req.getBookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", req.getBookingId()));
        if (!booking.getUser().getId().equals(userId))
            throw new BusinessException("Not authorized", "UNAUTHORIZED");
        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS)
            throw new BusinessException("Booking already paid", "ALREADY_PAID");

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject opts = new JSONObject();
            // Razorpay uses paise (multiply by 100)
            opts.put("amount", booking.getTotalAmount().multiply(new BigDecimal("100")).intValue());
            opts.put("currency", "INR");
            opts.put("receipt", "RC_" + booking.getPnr());
            opts.put("notes", new JSONObject().put("pnr", booking.getPnr()).put("booking_id", booking.getId().toString()));
            Order order = client.orders.create(opts);

            Payment payment = Payment.builder()
                .booking(booking)
                .paymentGateway(req.getPaymentGateway())
                .gatewayOrderId(order.get("id"))
                .amount(booking.getTotalAmount())
                .paymentMethod(req.getPaymentMethod())
                .status(PaymentStatus.PENDING).build();
            paymentRepository.save(payment);

            // Build UPI deep link for GPay/PhonePe
            String upiLink = buildUpiDeepLink(req.getPaymentGateway(), booking, order.get("id"));

            return PaymentOrderResponse.builder()
                .orderId(order.get("id"))
                .amount(booking.getTotalAmount())
                .currency("INR")
                .gatewayKeyId(razorpayKeyId)
                .bookingId(booking.getId().toString())
                .pnr(booking.getPnr())
                .gatewayName(req.getPaymentGateway().name())
                .upiDeepLink(upiLink)
                .build();
        } catch (Exception e) {
            log.error("Payment order creation failed", e);
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Transactional
    public void verifyAndConfirmPayment(String orderId, String paymentId, String signature) {
        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        // Verify Razorpay signature
        boolean valid = verifyRazorpaySignature(orderId, paymentId, signature);
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new PaymentException("Payment signature verification failed");
        }
        payment.setGatewayPaymentId(paymentId);
        payment.setGatewaySignature(signature);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);
        notificationService.sendPaymentConfirmation(booking, payment);
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }

    private String buildUpiDeepLink(PaymentGateway gateway, Booking booking, String orderId) {
        String pa = "railconnect@okaxis";
        String pn = "RailConnect";
        String amount = booking.getTotalAmount().toString();
        String tn = "Booking " + booking.getPnr();
        String baseUpi = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s", pa, pn, amount, tn);
        return switch (gateway) {
            case UPI_GPAY -> "gpay://upi/pay?" + baseUpi.substring(11);
            case PHONEPE -> "phonepe://" + baseUpi.substring(6);
            default -> baseUpi;
        };
    }
}
