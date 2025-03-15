package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing dashboard statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private int totalProjects;
    private int totalDependencies;
    private int outdatedDependencies;
    private int vulnerabilities;
    private List<HistoryResponse> recentAnalyses;
} 