package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.DashboardStatsResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.HistoryResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyAnalysisRepository;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyRepository;
import com.muratcan.yeldan.mavenanalyzer.repository.ProjectRepository;
import com.muratcan.yeldan.mavenanalyzer.repository.VulnerabilityRepository;
import com.muratcan.yeldan.mavenanalyzer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ProjectRepository projectRepository;
    private final DependencyRepository dependencyRepository;
    private final DependencyAnalysisRepository dependencyAnalysisRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        log.info("Collecting dashboard statistics");

        // Count projects
        long projectCount = projectRepository.count();
        log.debug("Total projects: {}", projectCount);

        // Count all dependencies across all analyses
        long dependencyCount = dependencyRepository.count();
        log.debug("Total dependencies: {}", dependencyCount);

        // Count outdated dependencies
        long outdatedCount = dependencyRepository.countByIsOutdated(true);
        log.debug("Outdated dependencies: {}", outdatedCount);

        // Count vulnerabilities
        long vulnerabilityCount = vulnerabilityRepository.count();
        log.debug("Total vulnerabilities: {}", vulnerabilityCount);

        // Get recent analyses (latest 3)
        List<DependencyAnalysis> recentAnalyses = dependencyAnalysisRepository.findAll(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "analysisDate"))
        ).getContent();

        // Map to response objects
        List<HistoryResponse> recentAnalysesResponses = recentAnalyses.stream()
                .map(this::mapToHistoryResponse)
                .toList();

        // Build and return the response
        return DashboardStatsResponse.builder()
                .totalProjects((int) projectCount)
                .totalDependencies((int) dependencyCount)
                .outdatedDependencies((int) outdatedCount)
                .vulnerabilities((int) vulnerabilityCount)
                .recentAnalyses(recentAnalysesResponses)
                .build();
    }

    /**
     * Map DependencyAnalysis entity to HistoryResponse DTO
     */
    private HistoryResponse mapToHistoryResponse(DependencyAnalysis analysis) {
        // Count total vulnerabilities
        int vulnerableCount = analysis.getDependencies().stream()
                .mapToInt(d -> d.getVulnerableCount() != null ? d.getVulnerableCount() : 0)
                .sum();

        return HistoryResponse.builder()
                .analysisId(analysis.getId())
                .projectId(analysis.getProject().getId())
                .projectName(analysis.getProject().getName())
                .analysisDate(analysis.getAnalysisDate())
                .totalDependencies(analysis.getTotalDependencies())
                .outdatedDependencies(analysis.getOutdatedDependencies())
                .upToDateDependencies(analysis.getUpToDateDependencies())
                .unidentifiedDependencies(analysis.getUnidentifiedDependencies())
                .vulnerableCount(vulnerableCount)
                .chartPath(analysis.getChartPath())
                .build();
    }
} 