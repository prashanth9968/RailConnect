package com.railconnect.service;

import com.railconnect.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Booking Confirmed - PNR: " + booking.getPnrNumber());
            helper.setText(buildBookingConfirmationHtml(booking), true);
            mailSender.send(message);
            log.info("Confirmation email sent for PNR: {}", booking.getPnrNumber());
        } catch (Exception e) {
            log.error("Failed to send confirmation email: {}", e.getMessage());
        }
    }

    @Async
    public void sendCancellationConfirmation(Booking booking, BigDecimal refundAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Booking Cancelled - PNR: " + booking.getPnrNumber());
            helper.setText(buildCancellationHtml(booking, refundAmount), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation email: {}", e.getMessage());
        }
    }

    @Async
    public void sendWaitlistConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Great News! Waitlist Confirmed - PNR: " + booking.getPnrNumber());
            helper.setText(buildWaitlistConfirmHtml(booking), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send waitlist confirmation: {}", e.getMessage());
        }
    }

    private String buildBookingConfirmationHtml(Booking b) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden">
              <div style="background:#1a237e;color:white;padding:20px;text-align:center">
                <h1 style="margin:0">🚂 RailConnect</h1>
                <p style="margin:5px 0">Booking Confirmed!</p>
              </div>
              <div style="padding:24px">
                <h2 style="color:#1a237e">PNR: %s</h2>
                <table style="width:100%%;border-collapse:collapse">
                  <tr><td style="padding:8px;border-bottom:1px solid #eee"><b>Train</b></td><td style="padding:8px;border-bottom:1px solid #eee">%s - %s</td></tr>
                  <tr><td style="padding:8px;border-bottom:1px solid #eee"><b>Journey Date</b></td><td style="padding:8px;border-bottom:1px solid #eee">%s</td></tr>
                  <tr><td style="padding:8px;border-bottom:1px solid #eee"><b>From → To</b></td><td style="padding:8px;border-bottom:1px solid #eee">%s → %s</td></tr>
                  <tr><td style="padding:8px"><b>Total Paid</b></td><td style="padding:8px">₹%s</td></tr>
                </table>
              </div>
              <div style="background:#f5f5f5;padding:16px;text-align:center;font-size:12px;color:#666">
                Have a safe journey! | RailConnect Support: support@railconnect.in
              </div>
            </div>
            """.formatted(
                b.getPnrNumber(), b.getTrain().getTrainNumber(), b.getTrain().getTrainName(),
                b.getJourneyDate(), b.getSourceStation().getStationName(), b.getDestinationStation().getStationName(),
                b.getTotalAmount()
            );
    }

    private String buildCancellationHtml(Booking b, BigDecimal refund) {
        return "<p>Your booking PNR <b>" + b.getPnrNumber() + "</b> has been cancelled.<br>Refund of <b>₹" + refund + "</b> will be credited in 5-7 business days.</p>";
    }

    private String buildWaitlistConfirmHtml(Booking b) {
        return "<p>Congratulations! Your waitlisted booking PNR <b>" + b.getPnrNumber() + "</b> is now CONFIRMED! Have a great journey.</p>";
    }
}
