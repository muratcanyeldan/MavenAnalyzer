package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.DashboardStatsResponse;

/**
 * Service for dashboard related functionality
 */
public interface DashboardService {

    /**
     * Get statistics for the dashboard
     *
     * @return Dashboard statistics response
     */
    DashboardStatsResponse getDashboardStats();
} 