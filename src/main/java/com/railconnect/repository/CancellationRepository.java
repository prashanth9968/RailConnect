package com.railconnect.repository;

import com.railconnect.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, UUID> {
    Optional<Cancellation> findByBookingId(UUID bookingId);
}
