package com.railconnect.service.impl;
import com.railconnect.dto.request.TrainSearchRequest;
import com.railconnect.dto.response.TrainSearchResponse;
import com.railconnect.entity.*;
import com.railconnect.enums.ClassType;
import com.railconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service @Slf4j @RequiredArgsConstructor
public class TrainSearchServiceImpl {
    private final TrainScheduleRepository scheduleRepository;
    private final StationRepository stationRepository;
    private final SeatInventoryRepository inventoryRepository;
    private final TrainRouteRepository trainRouteRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "trainSearch", key = "#req.fromStationCode + '_' + #req.toStationCode + '_' + #req.journeyDate")
    public List<TrainSearchResponse> searchTrains(TrainSearchRequest req) {
        Station fromStation = stationRepository.findByStationCode(req.getFromStationCode())
            .orElseThrow(() -> new com.railconnect.exception.ResourceNotFoundException("Station", "code", req.getFromStationCode()));
        Station toStation = stationRepository.findByStationCode(req.getToStationCode())
            .orElseThrow(() -> new com.railconnect.exception.ResourceNotFoundException("Station", "code", req.getToStationCode()));

        List<TrainSchedule> schedules = scheduleRepository.findSchedules(
            fromStation.getId(), toStation.getId(), req.getJourneyDate());

        return schedules.stream().map(schedule -> buildSearchResponse(schedule, fromStation, toStation, req))
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private TrainSearchResponse buildSearchResponse(TrainSchedule schedule, Station from, Station to, TrainSearchRequest req) {
        Train train = schedule.getTrain();
        List<SeatInventory> inventories = inventoryRepository.findByScheduleId(schedule.getId())
            .stream().filter(inv -> inv.getFromStation().getId().equals(from.getId())
                && inv.getToStation().getId().equals(to.getId()))
            .toList();

        List<TrainSearchResponse.ClassAvailability> classAvailability = inventories.stream()
            .filter(inv -> req.getPreferredClass() == null || inv.getClassType() == req.getPreferredClass())
            .map(inv -> TrainSearchResponse.ClassAvailability.builder()
                .classType(inv.getClassType().name())
                .className(inv.getClassType().getDisplayName())
                .availableSeats(inv.getAvailableSeats())
                .waitlistedCount(inv.getWaitlistedCount())
                .tatkalAvailable(inv.getTatkalAvailable())
                .premiumTatkalAvailable(inv.getPremiumTatkalAvailable())
                .fare(inv.getFare()).tatkalFare(inv.getTatkalFare())
                .premiumTatkalFare(inv.getPremiumTatkalFare()).build())
            .collect(Collectors.toList());

        // Get departure/arrival times from route
        var fromRoute = train.getRoutes() != null ? train.getRoutes().stream()
            .filter(r -> r.getStation().getId().equals(from.getId())).findFirst() : Optional.empty();
        var toRoute = train.getRoutes() != null ? train.getRoutes().stream()
            .filter(r -> r.getStation().getId().equals(to.getId())).findFirst() : Optional.empty();

        String departure = fromRoute.map(r -> r.getDepartureTime() != null ? r.getDepartureTime().toString() : "").orElse("--:--");
        String arrival = toRoute.map(r -> r.getArrivalTime() != null ? r.getArrivalTime().toString() : "").orElse("--:--");

        return TrainSearchResponse.builder()
            .scheduleId(schedule.getId()).trainId(train.getId())
            .trainNumber(train.getTrainNumber()).trainName(train.getTrainName())
            .trainType(train.getTrainType().name())
            .fromStation(from.getStationName()).fromStationCode(from.getStationCode())
            .toStation(to.getStationName()).toStationCode(to.getStationCode())
            .departureTime(departure).arrivalTime(arrival)
            .journeyDate(schedule.getJourneyDate().toString())
            .status(schedule.getStatus().name()).delayMinutes(schedule.getDelayMinutes())
            .pantryAvailable(train.isPantryAvailable())
            .classAvailability(classAvailability).build();
    }
}
