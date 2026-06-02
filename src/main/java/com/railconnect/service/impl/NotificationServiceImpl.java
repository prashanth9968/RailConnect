package com.railconnect.service.impl;
import com.railconnect.entity.*;
import com.railconnect.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.internet.MimeMessage;
@Service @Slf4j @RequiredArgsConstructor
public class NotificationServiceImpl {
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Async @Transactional
    public void sendBookingConfirmation(Booking booking) {
        Notification notification = Notification.builder()
            .user(booking.getUser())
            .title("Booking Confirmed - PNR: " + booking.getPnr())
            .message(buildBookingMessage(booking))
            .notificationType("BOOKING_CONFIRMED")
            .referenceId(booking.getId()).build();
        notificationRepository.save(notification);
        sendEmail(booking.getUser().getEmail(), "Booking Confirmation - " + booking.getPnr(),
            buildBookingEmailHtml(booking));
    }

    @Async @Transactional
    public void sendPaymentConfirmation(Booking booking, Payment payment) {
        Notification notification = Notification.builder()
            .user(booking.getUser())
            .title("Payment Successful - ₹" + payment.getAmount())
            .message("Payment of ₹" + payment.getAmount() + " for PNR " + booking.getPnr() + " received.")
            .notificationType("PAYMENT_SUCCESS")
            .referenceId(booking.getId()).build();
        notificationRepository.save(notification);
    }

    @Async @Transactional
    public void sendCancellationConfirmation(Booking booking) {
        Notification notification = Notification.builder()
            .user(booking.getUser())
            .title("Booking Cancelled - PNR: " + booking.getPnr())
            .message("Your booking " + booking.getPnr() + " has been cancelled. " +
                (booking.getRefundAmount() != null && booking.getRefundAmount().signum() > 0 ?
                    "Refund of ₹" + booking.getRefundAmount() + " will be processed in 5-7 days." : "No refund applicable."))
            .notificationType("BOOKING_CANCELLED")
            .referenceId(booking.getId()).build();
        notificationRepository.save(notification);
    }

    private String buildBookingMessage(Booking b) {
        return String.format("Train: %s | %s → %s | Date: %s | PNR: %s | Amount: ₹%s",
            b.getSchedule().getTrain().getTrainName(),
            b.getFromStation().getStationName(), b.getToStation().getStationName(),
            b.getSchedule().getJourneyDate(), b.getPnr(), b.getTotalAmount());
    }

    private String buildBookingEmailHtml(Booking b) {
        return "<html><body style='font-family:Arial'>" +
            "<h2 style='color:#1a3a6e'>🚂 RailConnect - Booking Confirmation</h2>" +
            "<table border='1' cellpadding='8' style='border-collapse:collapse'>" +
            "<tr><th>PNR</th><td><b>" + b.getPnr() + "</b></td></tr>" +
            "<tr><th>Train</th><td>" + b.getSchedule().getTrain().getTrainNumber() + " - " + b.getSchedule().getTrain().getTrainName() + "</td></tr>" +
            "<tr><th>Journey</th><td>" + b.getFromStation().getStationName() + " → " + b.getToStation().getStationName() + "</td></tr>" +
            "<tr><th>Date</th><td>" + b.getSchedule().getJourneyDate() + "</td></tr>" +
            "<tr><th>Class</th><td>" + b.getClassType().getDisplayName() + "</td></tr>" +
            "<tr><th>Total Amount</th><td>₹" + b.getTotalAmount() + "</td></tr>" +
            "</table></body></html>";
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to); helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Email send failed to {}: {}", to, e.getMessage());
        }
    }
}
