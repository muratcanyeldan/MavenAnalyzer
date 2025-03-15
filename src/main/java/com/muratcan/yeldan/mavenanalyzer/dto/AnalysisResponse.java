package com.muratcan.yeldan.mavenanalyzer.dto;

import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis.VulnerabilityCheckStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private LocalDateTime analysisDate;
    private Integer totalDependencies;
    private Integer outdatedDependencies;
    private Integer upToDateDependencies;
    private Integer unidentifiedDependencies;
    private String chartPath;
    private VulnerabilityCheckStatus vulnerabilityCheckStatus;
    private List<DependencyResponse> dependencies;

    // Add vulnerable count
    private Integer vulnerableCount;

    // Add license issues count
    private Integer licenseIssues;

    // Add notifyOnCompletion field
    private boolean notifyOnCompletion;
} 