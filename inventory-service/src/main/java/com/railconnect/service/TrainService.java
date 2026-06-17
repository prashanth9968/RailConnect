package com.railconnect.service;

import com.railconnect.dto.request.TrainSearchRequest;
import com.railconnect.dto.response.*;
import com.railconnect.entity.*;
import com.railconnect.enums.SeatClass;
import com.railconnect.enums.SeatStatus;
import com.railconnect.enums.ClassType;
import com.railconnect.enums.QuotaType;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.*;
import com.railconnect.util.FareCalculator;
import com.railconnect.util.OccupancyCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;
    private final SeatRepository seatRepository;
    private final TrainRouteRepository routeRepository;
    private final FareCalculator fareCalculator;
    private final TrainAvailabilityRepository trainAvailabilityRepository;

    @Cacheable(value = "trains", key = "#request.fromStation + '_' + #request.toStation + '_' + #request.journeyDate")
    public List<TrainSearchResponse> searchTrains(TrainSearchRequest request) {
        Station fromStation = stationRepository.findByStationCode(request.getFromStation())
            .orElseThrow(() -> new RailConnectException("Source station not found", HttpStatus.NOT_FOUND));
        Station toStation = stationRepository.findByStationCode(request.getToStation())
            .orElseThrow(() -> new RailConnectException("Destination station not found", HttpStatus.NOT_FOUND));

        int dayBit = getDayBit(request.getJourneyDate().getDayOfWeek());
        List<Train> trains = trainRepository.findTrainsBetweenStations(
            fromStation.getStationCode(), toStation.getStationCode(), dayBit);

        return trains.stream()
            .map(train -> buildSearchResponse(train, fromStation, toStation, request))
            .filter(Objects::nonNull)
            .toList();
    }

    @Cacheable(value = "availability", key = "#trainId + '_' + #date + '_' + #seatClass")
    @Transactional
    public Map<String, AvailabilityInfo> getAvailability(Long trainId, LocalDate date, SeatClass seatClass) {
        Train train = trainRepository.findById(trainId)
            .orElseThrow(() -> new RailConnectException("Train not found", HttpStatus.NOT_FOUND));

        ensureAvailabilityGenerated(train, date);

        Map<String, AvailabilityInfo> result = new LinkedHashMap<>();
        List<SeatClass> classes = seatClass != null ? List.of(seatClass) : List.of(SeatClass.values());

        Set<SeatClass> trainClasses = new HashSet<>();
        for (TrainCoach coach : train.getCoaches()) {
            trainClasses.add(coach.getSeatClass());
        }

        for (SeatClass sc : classes) {
            if (!trainClasses.contains(sc)) continue;

            ClassType classType = mapToClassType(sc);
            Optional<TrainAvailability> taOpt = trainAvailabilityRepository
                .findByTrainIdAndJourneyDateAndClassTypeAndQuotaType(trainId, date, classType, QuotaType.GENERAL);

            if (taOpt.isPresent()) {
                TrainAvailability ta = taOpt.get();
                int available = ta.getAvailableSeats();
                int rac = ta.getRacCount();
                int wl = ta.getWaitingListCount();
                int tatkal = available > 0 ? available / 3 : 0;

                result.put(sc.name(), AvailabilityInfo.builder()
                    .availableSeats(available)
                    .racSeats(rac)
                    .waitlistCount(wl)
                    .tatkalAvailable(tatkal)
                    .premiumTatkalAvailable(tatkal > 0 ? tatkal / 2 : 0)
                    .baseFare(ta.getBaseFare())
                    .tatkalFare(ta.getTatkalFare())
                    .premiumTatkalFare(ta.getTatkalFare().multiply(java.math.BigDecimal.valueOf(1.3)))
                    .status(available > 0 ? "AVAILABLE" : rac > 0 ? "RAC" : wl > 0 ? "WL" : "NOT_AVAILABLE")
                    .build());
            }
        }
        return result;
    }

    @Transactional
    public void ensureAvailabilityGenerated(Train train, LocalDate date) {
        Set<SeatClass> classes = new HashSet<>();
        for (TrainCoach coach : train.getCoaches()) {
            classes.add(coach.getSeatClass());
        }

        for (SeatClass sc : classes) {
            ClassType classType = mapToClassType(sc);
            Optional<TrainAvailability> existing = trainAvailabilityRepository
                .findByTrainIdAndJourneyDateAndClassTypeAndQuotaType(train.getId(), date, classType, QuotaType.GENERAL);

            if (existing.isEmpty()) {
                int totalSeats = train.getCoaches().stream()
                    .filter(c -> c.getSeatClass() == sc)
                    .mapToInt(TrainCoach::getTotalSeats)
                    .sum();

                double occupancy = OccupancyCalculator.calculateOccupancy(train.getTrainType(), date);
                int available = totalSeats - (int) Math.round(totalSeats * occupancy);

                TrainAvailability ta = TrainAvailability.builder()
                    .train(train)
                    .journeyDate(date)
                    .classType(classType)
                    .quotaType(QuotaType.GENERAL)
                    .totalSeats(totalSeats)
                    .availableSeats(available)
                    .baseFare(fareCalculator.calculateBaseFare(sc, 500, 1))
                    .tatkalFare(fareCalculator.calculateTatkalCharge(sc, 500, 1))
                    .build();
                trainAvailabilityRepository.save(ta);
            }
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

    @Cacheable(value = "stations", key = "#query")
    public List<Station> searchStations(String query) {
        return stationRepository.searchStations(query);
    }

    private TrainSearchResponse buildSearchResponse(Train train, Station from, Station to, TrainSearchRequest req) {
        List<TrainRoute> routes = routeRepository.findByTrainIdOrderByStopNumber(train.getId());
        TrainRoute fromRoute = routes.stream().filter(r -> r.getStation().getStationCode().equals(from.getStationCode())).findFirst().orElse(null);
        TrainRoute toRoute = routes.stream().filter(r -> r.getStation().getStationCode().equals(to.getStationCode())).findFirst().orElse(null);
        if (fromRoute == null || toRoute == null) return null;

        int distanceKm = toRoute.getDistanceFromSource() - fromRoute.getDistanceFromSource();
        Map<String, AvailabilityInfo> availability = getAvailability(train.getId(), req.getJourneyDate(), req.getSeatClass());

        return TrainSearchResponse.builder()
            .trainId(train.getId())
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .trainType(train.getTrainType())
            .sourceStationCode(from.getStationCode())
            .sourceStationName(from.getStationName())
            .departureTime(fromRoute.getDepartureTime())
            .destinationStationCode(to.getStationCode())
            .destinationStationName(to.getStationName())
            .arrivalTime(toRoute.getArrivalTime())
            .duration(calculateDuration(fromRoute, toRoute))
            .dayDifference(toRoute.getDayNumber() - fromRoute.getDayNumber())
            .classAvailability(availability)
            .build();
    }

    private String calculateDuration(TrainRoute from, TrainRoute to) {
        long fromMins = from.getDepartureTime().toSecondOfDay() / 60;
        long toMins = to.getArrivalTime().toSecondOfDay() / 60 + (to.getDayNumber() - from.getDayNumber()) * 1440L;
        long diff = toMins - fromMins;
        return (diff / 60) + "h " + (diff % 60) + "m";
    }

    private int getDayBit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }

    private int getTatkalAvailability(Long trainId, LocalDate date, SeatClass sc) {
        if (!fareCalculator.isTatkalBookingAllowed(date)) return 0;
        return seatRepository.countByTrainDateClassStatus(trainId, date, sc, SeatStatus.AVAILABLE) / 3;
    }
}
