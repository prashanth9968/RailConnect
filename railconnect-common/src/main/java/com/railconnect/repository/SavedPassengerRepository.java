package com.railconnect.repository;

import com.railconnect.entity.SavedPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavedPassengerRepository extends JpaRepository<SavedPassenger, UUID> {
    List<SavedPassenger> findByUserId(UUID userId);
}
