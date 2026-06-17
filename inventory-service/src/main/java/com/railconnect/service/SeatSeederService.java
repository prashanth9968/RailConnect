package com.railconnect.service;

import com.railconnect.entity.*;
import com.railconnect.enums.SeatClass;
import com.railconnect.enums.SeatStatus;
import com.railconnect.enums.ClassType;
import com.railconnect.enums.QuotaType;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.*;
import com.railconnect.util.OccupancyCalculator;
import com.railconnect.util.FareCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final TrainAvailabilityRepository trainAvailabilityRepository;
    private final FareCalculator fareCalculator;

    /**
     * Generates physical seat records in the database for the coaches of a train on a given date on demand,
     * ensuring that availability counts align exactly with the TrainAvailability configuration.
     */
    @Transactional
    public void ensureSeatsGenerated(Long trainId, LocalDate date) {
        Train train = trainRepository.findById(trainId)
                .orElseThrow(() -> new RailConnectException("Train not found", org.springframework.http.HttpStatus.NOT_FOUND));

        if (train.getCoaches().isEmpty()) return;

        // Check if seats already exist for this train and date (using the first coach)
        TrainCoach firstCoach = train.getCoaches().get(0);
        List<Seat> existing = seatRepository.findByCoachAndDate(firstCoach.getId(), date);
        if (!existing.isEmpty()) {
            return; // Already generated
        }

        log.info("Generating seats on demand for train {} on date {}", train.getTrainNumber(), date);
        List<Seat> toSave = new ArrayList<>();

        for (TrainCoach coach : train.getCoaches()) {
            ClassType classType = mapToClassType(coach.getSeatClass());
            Optional<TrainAvailability> taOpt = trainAvailabilityRepository
                    .findByTrainIdAndJourneyDateAndClassTypeAndQuotaType(trainId, date, classType, QuotaType.GENERAL);

            int totalSeats = coach.getTotalSeats();
            int availableSeats = totalSeats;

            if (taOpt.isPresent()) {
                TrainAvailability ta = taOpt.get();
                double availableRatio = (double) ta.getAvailableSeats() / ta.getTotalSeats();
                availableSeats = (int) Math.round(totalSeats * availableRatio);
            } else {
                // Generate availability record lazily if missing
                double occupancy = OccupancyCalculator.calculateOccupancy(train.getTrainType(), date);
                availableSeats = totalSeats - (int) Math.round(totalSeats * occupancy);

                TrainAvailability ta = TrainAvailability.builder()
                        .train(train)
                        .journeyDate(date)
                        .classType(classType)
                        .quotaType(QuotaType.GENERAL)
                        .totalSeats(totalSeats)
                        .availableSeats(availableSeats)
                        .baseFare(fareCalculator.calculateBaseFare(coach.getSeatClass(), 500, 1))
                        .tatkalFare(fareCalculator.calculateTatkalCharge(coach.getSeatClass(), 500, 1))
                        .build();
                trainAvailabilityRepository.save(ta);
            }

            // Create seat rows, randomizing which ones are booked to match availability count
            List<Integer> seatIndexes = new ArrayList<>();
            for (int i = 1; i <= totalSeats; i++) {
                seatIndexes.add(i);
            }
            Collections.shuffle(seatIndexes);

            Set<Integer> bookedSeats = new HashSet<>();
            int bookedCount = totalSeats - availableSeats;
            for (int i = 0; i < bookedCount && i < seatIndexes.size(); i++) {
                bookedSeats.add(seatIndexes.get(i));
            }

            for (int i = 1; i <= totalSeats; i++) {
                String berthType = getBerthType(coach.getSeatClass().name(), i);
                SeatStatus status = bookedSeats.contains(i) ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;

                toSave.add(Seat.builder()
                        .coach(coach)
                        .seatNumber(String.valueOf(i))
                        .berthType(berthType)
                        .status(status)
                        .journeyDate(date)
                        .build());
            }
        }

        if (!toSave.isEmpty()) {
            seatRepository.saveAll(toSave);
            log.info("Successfully generated {} seats for train {} on date {}", toSave.size(), train.getTrainNumber(), date);
        }
    }

    private ClassType mapToClassType(SeatClass seatClass) {
        return switch (seatClass) {
            case SL -> ClassType.SL;
            case S3 -> ClassType.THIRD_AC;
            case S2 -> ClassType.SECOND_AC;
            case S1 -> ClassType.FIRST_AC;
            case CC -> ClassType.AC_CHAIR_CAR;
            case EC -> ClassType.EXECUTIVE_CHAIR;
            default -> ClassType.SECOND_SEATING;
        };
    }

    public void seedSeatsForFutureDates() {
        log.info("Manual seat seeding triggered (No-op since dynamic lazy generation is active).");
    }

    private String getBerthType(String seatClass, int seatNum) {
        if (seatClass.equals("GN") || seatClass.equals("CC") || seatClass.equals("EC")) {
            return seatNum % 2 == 0 ? "WINDOW" : "AISLE";
        }
        // sleeper / AC coaches: 8 seats per bay (1-6 main, 7-8 side)
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
