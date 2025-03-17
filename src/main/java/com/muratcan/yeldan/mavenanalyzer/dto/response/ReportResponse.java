package com.muratcan.yeldan.mavenanalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String reportPath;
    private String fileName;
    private String generatedAt;
    private String projectName;
    private String analysisDate;
    private Integer totalDependencies;
    private Integer outdatedDependencies;
    private Integer vulnerableDependencies;
} 