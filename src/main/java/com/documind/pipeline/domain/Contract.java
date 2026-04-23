package com.documind.pipeline.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ContractStatus status;

    // Advanced JSONB mapping using Hibernate 6 specific native support
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", columnDefinition = "jsonb")
    private String extractedData;
}
