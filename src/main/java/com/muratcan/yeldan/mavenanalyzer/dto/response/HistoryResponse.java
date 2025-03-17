package com.muratcan.yeldan.mavenanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {

    private Long analysisId;
    private Long projectId;
    private String projectName;
    private LocalDateTime analysisDate;
    private Integer totalDependencies;
    private Integer outdatedDependencies;
    private Integer upToDateDependencies;
    private Integer unidentifiedDependencies;
    private Integer vulnerableCount;
    private Integer licenseIssues;
    private String chartPath;
} 