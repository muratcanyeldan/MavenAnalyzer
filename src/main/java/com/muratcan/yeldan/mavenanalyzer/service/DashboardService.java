package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.response.DashboardStatsResponse;

public interface DashboardService {

    /**
     * Get statistics for the dashboard
     *
     * @return Dashboard statistics response
     */
    DashboardStatsResponse getDashboardStats();
} 