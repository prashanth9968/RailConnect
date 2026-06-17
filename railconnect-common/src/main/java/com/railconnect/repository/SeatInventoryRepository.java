package com.railconnect.repository;
import com.railconnect.entity.SeatInventory;
import com.railconnect.enums.ClassType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, UUID> {
    List<SeatInventory> findByScheduleId(Long scheduleId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInventory si WHERE si.schedule.id = :scheduleId AND si.classType = :classType AND si.fromStation.id = :fromId AND si.toStation.id = :toId")
    Optional<SeatInventory> findAndLock(@Param("scheduleId") Long scheduleId,
                                        @Param("classType") ClassType classType,
                                        @Param("fromId") UUID fromId,
                                        @Param("toId") UUID toId);
    @Modifying
    @Query("UPDATE SeatInventory si SET si.availableSeats = si.availableSeats - :count WHERE si.id = :id AND si.availableSeats >= :count")
    int decrementAvailableSeats(@Param("id") UUID id, @Param("count") int count);
    @Modifying
    @Query("UPDATE SeatInventory si SET si.availableSeats = si.availableSeats + :count WHERE si.id = :id")
    void incrementAvailableSeats(@Param("id") UUID id, @Param("count") int count);
    @Modifying
    @Query("UPDATE SeatInventory si SET si.waitlistedCount = si.waitlistedCount + 1 WHERE si.id = :id")
    void incrementWaitlist(@Param("id") UUID id);
}
