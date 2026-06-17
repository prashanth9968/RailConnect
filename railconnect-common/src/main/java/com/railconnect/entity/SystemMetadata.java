package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_metadata")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemMetadata {
    @Id
    @Column(name = "metadata_key")
    private String key;

    @Column(name = "metadata_value", nullable = false)
    private String value;
}
