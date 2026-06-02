package com.railconnect.repository;
import com.railconnect.entity.TrainLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface TrainLocationHistoryRepository extends JpaRepository<TrainLocationHistory, UUID> {
    List<TrainLocationHistory> findTop10ByScheduleIdOrderByRecordedAtDesc(UUID scheduleId);
    Optional<TrainLocationHistory> findFirstByScheduleIdOrderByRecordedAtDesc(UUID scheduleId);
}
