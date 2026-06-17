package com.railconnect.repository;

import com.railconnect.entity.SystemMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemMetadataRepository extends JpaRepository<SystemMetadata, String> {
}
