package com.railconnect.seeder;

import com.railconnect.entity.*;
import com.railconnect.enums.SeatClass;
import com.railconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataSeeder implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final SystemMetadataRepository metadataRepository;

    public static final String MASTER_DATA_VERSION = "1";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Optional<SystemMetadata> metaOpt = metadataRepository.findById("master_data_version");
        if (metaOpt.isPresent() && MASTER_DATA_VERSION.equals(metaOpt.get().getValue())) {
            log.info("Master data version {} already loaded. Skipping seeder.", MASTER_DATA_VERSION);
            return;
        }

        log.info("Starting master data seeder (Version {})...", MASTER_DATA_VERSION);

        log.info("Cleaning up old seed data...");
        trainRepository.deleteAll();
        trainRepository.flush();
        stationRepository.deleteAll();
        stationRepository.flush();

        // 1. Seed Stations
        Map<String, Station> stationMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("master-data/stations.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            List<Station> stationsBatch = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 7) continue;

                Station s = Station.builder()
                        .stationCode(cols[0])
                        .stationName(cols[1])
                        .city(cols[2])
                        .state(cols[3])
                        .zone(cols[4])
                        .latitude(Double.parseDouble(cols[5]))
                        .longitude(Double.parseDouble(cols[6]))
                        .active(cols.length > 7 ? Boolean.parseBoolean(cols[7]) : true)
                        .build();
                stationsBatch.add(s);

                if (stationsBatch.size() >= 100) {
                    List<Station> saved = stationRepository.saveAll(stationsBatch);
                    saved.forEach(st -> stationMap.put(st.getStationCode(), st));
                    stationsBatch.clear();
                }
            }
            if (!stationsBatch.isEmpty()) {
                List<Station> saved = stationRepository.saveAll(stationsBatch);
                saved.forEach(st -> stationMap.put(st.getStationCode(), st));
            }
            log.info("Successfully seeded {} stations.", stationMap.size());
        }

        // 2. Seed Trains
        Map<String, Train> trainMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("master-data/trains.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 4) continue;

                Train t = Train.builder()
                        .trainNumber(cols[0])
                        .trainName(cols[1])
                        .trainType(cols[2])
                        .runningDays(Integer.parseInt(cols[3]))
                        .active(cols.length > 4 ? Boolean.parseBoolean(cols[4]) : true)
                        .routes(new ArrayList<>())
                        .coaches(new ArrayList<>())
                        .build();
                trainMap.put(t.getTrainNumber(), t);
            }
            log.info("Parsed {} trains.", trainMap.size());
        }

        // 3. Parse Routes
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("master-data/routes.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 8) continue;

                Train train = trainMap.get(cols[0]);
                Station station = stationMap.get(cols[1]);
                if (train != null && station != null) {
                    boolean alreadyHas = train.getRoutes().stream()
                            .anyMatch(r -> r.getStation().getStationCode().equalsIgnoreCase(station.getStationCode()));
                    if (alreadyHas) {
                        log.warn("Skipping duplicate route stop for train {} and station {}", train.getTrainNumber(), station.getStationCode());
                        continue;
                    }
                    TrainRoute route = TrainRoute.builder()
                            .train(train)
                            .station(station)
                            .stopNumber(Integer.parseInt(cols[2]))
                            .arrivalTime(cols[3].isEmpty() ? null : LocalTime.parse(cols[3]))
                            .departureTime(cols[4].isEmpty() ? null : LocalTime.parse(cols[4]))
                            .distanceFromSource(Integer.parseInt(cols[5]))
                            .dayNumber(Integer.parseInt(cols[6]))
                            .haltMinutes(Integer.parseInt(cols[7]))
                            .build();
                    train.getRoutes().add(route);
                    count++;
                }
            }
            log.info("Parsed {} route stops.", count);
        }

        // 4. Parse Coaches
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("master-data/coaches.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 10) continue;

                Train train = trainMap.get(cols[0]);
                if (train != null) {
                    TrainCoach coach = TrainCoach.builder()
                            .train(train)
                            .coachNumber(cols[1])
                            .seatClass(SeatClass.valueOf(cols[2]))
                            .totalSeats(Integer.parseInt(cols[3]))
                            .tatkalQuota(Integer.parseInt(cols[4]))
                            .premiumTatkalQuota(Integer.parseInt(cols[5]))
                            .ladiesQuota(Integer.parseInt(cols[6]))
                            .seniorCitizenQuota(Integer.parseInt(cols[7]))
                            .maxRacSeats(Integer.parseInt(cols[8]))
                            .maxWaitlistSeats(Integer.parseInt(cols[9]))
                            .build();
                    train.getCoaches().add(coach);
                    count++;
                }
            }
            log.info("Parsed {} coach assignments.", count);
        }

        // 5. Save all trains cascadingly
        log.info("Saving trains to database...");
        trainRepository.saveAll(trainMap.values());
        log.info("Successfully saved {} trains to database.", trainMap.size());

        // Save metadata version to prevent re-seeding
        SystemMetadata meta = SystemMetadata.builder()
                .key("master_data_version")
                .value(MASTER_DATA_VERSION)
                .build();
        metadataRepository.save(meta);

        log.info("Master data seeder finished successfully.");
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString().trim());
        return result.toArray(new String[0]);
    }
}
