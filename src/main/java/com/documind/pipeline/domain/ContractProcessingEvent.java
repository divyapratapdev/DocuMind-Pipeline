package com.documind.pipeline.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractProcessingEvent {
    private UUID contractId;
    private String s3ObjectKey;
    private UUID organizationId;
}
