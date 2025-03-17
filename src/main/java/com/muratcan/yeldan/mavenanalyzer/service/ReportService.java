package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.response.ReportResponse;

public interface ReportService {

    /**
     * Generate a full PDF report for a dependency analysis
     *
     * @param analysisId The ID of the analysis to generate a report for
     * @return ReportResponse containing the report file path and metadata
     */
    ReportResponse generateFullReport(Long analysisId);
} 