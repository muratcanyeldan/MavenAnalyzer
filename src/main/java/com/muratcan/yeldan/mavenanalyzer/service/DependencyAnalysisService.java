package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.request.AnalysisRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.response.AnalysisResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.DependencyResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.HistoryResponse;

import java.util.List;

public interface DependencyAnalysisService {

    AnalysisResponse analyzeDependencies(AnalysisRequest request);

    AnalysisResponse getAnalysisById(Long id);

    List<HistoryResponse> getAnalysisHistoryByProjectId(Long projectId);

    AnalysisResponse getLatestAnalysisByProjectId(Long projectId);

    void deleteAnalysis(Long id);

    List<HistoryResponse> getAllAnalysisHistory();

    /**
     * Update a dependency's version
     *
     * @param dependencyId The ID of the dependency to update
     * @param newVersion   The new version to set
     * @return The updated dependency response
     */
    DependencyResponse updateDependencyVersion(Long dependencyId, String newVersion);

    /**
     * Generate an updated POM file with the latest non-BOM-managed dependency versions
     *
     * @param analysisId The ID of the analysis to generate the POM file for
     * @return The content of the updated POM file as a byte array
     */
    byte[] generateUpdatedPomFile(Long analysisId);
} 