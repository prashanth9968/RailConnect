package com.railconnect.service;

import com.railconnect.entity.*;
import com.railconnect.enums.SeatStatus;
import com.railconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatSeederService {

    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;

    /**
     * Every day at 00:30, pre-create seat rows for 120 days ahead.
     * This is required so availability queries work without dynamic seat creation.
     */
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void seedSeatsForFutureDates() {
        LocalDate target = LocalDate.now().plusDays(120);
        log.info("Seeding seats for date: {}", target);

        List<Train> trains = trainRepository.findAll();
        List<Seat> toSave = new ArrayList<>();

        for (Train train : trains) {
            for (TrainCoach coach : train.getCoaches()) {
                // Check if already seeded
                List<Seat> existing = seatRepository.findByCoachAndDate(coach.getId(), target);
                if (!existing.isEmpty()) continue;

                for (int i = 1; i <= coach.getTotalSeats(); i++) {
                    String berthType = getBerthType(coach.getSeatClass().name(), i);
                    toSave.add(Seat.builder()
                        .coach(coach)
                        .seatNumber(String.valueOf(i))
                        .berthType(berthType)
                        .status(SeatStatus.AVAILABLE)
                        .journeyDate(target)
                        .build());
                }
            }
        }

        if (!toSave.isEmpty()) {
            seatRepository.saveAll(toSave);
            log.info("Seeded {} seats for {}", toSave.size(), target);
        }
    }

    private String getBerthType(String seatClass, int seatNum) {
        if (seatClass.equals("GN") || seatClass.equals("CC") || seatClass.equals("EC")) {
            return seatNum % 2 == 0 ? "WINDOW" : "AISLE";
        }
        // For sleeper / AC coaches: 8 seats per bay (1-6 main, 7-8 side)
        int posInBay = ((seatNum - 1) % 8) + 1;
        return switch (posInBay) {
            case 1, 4 -> "LOWER";
            case 2, 5 -> "MIDDLE";
            case 3, 6 -> "UPPER";
            case 7 -> "SIDE_LOWER";
            case 8 -> "SIDE_UPPER";
            default -> "LOWER";
        };
    }
}
