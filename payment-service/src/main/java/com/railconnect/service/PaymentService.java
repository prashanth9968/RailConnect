package com.railconnect.service;

import com.railconnect.dto.request.*;
import com.railconnect.dto.response.*;
import com.railconnect.entity.*;
import com.railconnect.entity.Payment;
import com.railconnect.enums.*;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.*;
import com.razorpay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.railconnect.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RazorpayClient razorpayClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentInitResponse initiatePayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new RailConnectException("Booking not found", HttpStatus.NOT_FOUND));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RailConnectException("Cannot pay for cancelled booking", HttpStatus.BAD_REQUEST);
        }

        try {
            // Create Razorpay order
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", booking.getTotalAmount().multiply(new BigDecimal("100")).intValue()); // paise
            orderReq.put("currency", "INR");
            orderReq.put("receipt", booking.getPnrNumber());
            orderReq.put("payment_capture", 1);

            // Add notes for UPI
            JSONObject notes = new JSONObject();
            notes.put("pnr", booking.getPnrNumber());
            notes.put("passenger_name", booking.getPassengers().get(0).getName());
            orderReq.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderReq);

            Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .amount(booking.getTotalAmount())
                .razorpayOrderId(razorpayOrder.get("id"))
                .upiVpa(request.getUpiVpa())
                .build();
            paymentRepository.save(payment);

            // Build UPI intent URL for GPay/PhonePe
            String upiIntentUrl = buildUpiIntentUrl(request.getPaymentMethod(),
                razorpayOrder.get("id"), booking.getTotalAmount(), booking.getPnrNumber());

            return PaymentInitResponse.builder()
                .razorpayOrderId(razorpayOrder.get("id"))
                .razorpayKeyId(razorpayKeyId)
                .amount(booking.getTotalAmount())
                .currency("INR")
                .pnrNumber(booking.getPnrNumber())
                .upiIntentUrl(upiIntentUrl)
                .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RailConnectException("Payment gateway error. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Transactional
    public BookingResponse verifyPayment(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new RailConnectException("Payment not found", HttpStatus.NOT_FOUND));

        // Verify signature
        if (!verifyRazorpaySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new RailConnectException("Payment verification failed. Possible fraud attempt.", HttpStatus.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        // Update booking status
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        bookingRepository.save(booking);

        try {
            kafkaTemplate.send(
                KafkaConfig.TICKET_CONFIRMED_TOPIC,
                Map.of("bookingId", booking.getId().toString(), "status", "CONFIRMED")
            );
            kafkaTemplate.send(
                KafkaConfig.PAYMENT_COMPLETED_TOPIC,
                Map.of("bookingId", booking.getId().toString(), "paymentId", payment.getId().toString(), "amount", payment.getAmount().toString())
            );
        } catch (Exception e) {
            log.error("Failed to publish payment verification events to Kafka: {}", e.getMessage());
        }

        log.info("Payment verified for PNR: {}", booking.getPnrNumber());

        return toBookingResponse(booking);
    }

    @Transactional
    public void processRefund(UUID bookingId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new RailConnectException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("Refund skipped - payment not successful for booking: {}", bookingId);
            return;
        }

        try {
            JSONObject refundReq = new JSONObject();
            refundReq.put("amount", refundAmount.multiply(new BigDecimal("100")).intValue());
            refundReq.put("notes", new JSONObject().put("reason", "Customer cancellation"));

            Refund refund = razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundReq);
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refundAmount);
            payment.setRefundId(refund.get("id"));
            payment.setRefundedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Refund processed: {} for booking {}", refund.get("id"), bookingId);

        } catch (RazorpayException e) {
            log.error("Refund failed for booking {}: {}", bookingId, e.getMessage());
        }
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes());
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String buildUpiIntentUrl(PaymentMethod method, String orderId, BigDecimal amount, String pnr) {
        String pa = "railconnect@razorpay";
        String pn = "RailConnect";
        String am = amount.toPlainString();
        String tn = "Train Ticket " + pnr;

        String baseUrl = "upi://pay?pa=" + pa + "&pn=" + pn + "&am=" + am + "&cu=INR&tn=" + tn;

        return switch (method) {
            case UPI_GPAY -> "gpay://upi/pay?pa=" + pa + "&pn=" + pn + "&am=" + am + "&cu=INR&tn=" + tn;
            case UPI_PHONEPE -> "phonepe://pay?pa=" + pa + "&pn=" + pn + "&am=" + am + "&cu=INR&tn=" + tn;
            default -> baseUrl;
        };
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
            .id(b.getId())
            .pnrNumber(b.getPnrNumber())
            .trainNumber(b.getTrain().getTrainNumber())
            .trainName(b.getTrain().getTrainName())
            .journeyDate(b.getJourneyDate())
            .status(b.getStatus())
            .totalAmount(b.getTotalAmount())
            .paymentStatus(b.getPayment().getStatus())
            .bookedAt(b.getBookedAt())
            .build();
    }
}
