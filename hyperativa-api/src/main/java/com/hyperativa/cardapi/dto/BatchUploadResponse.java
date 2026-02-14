package com.hyperativa.cardapi.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchUploadResponse {
    private String batchId;
    private int totalProcessed;
    private int totalSuccess;
    private int totalErrors;
    private List<String> errors;
}
