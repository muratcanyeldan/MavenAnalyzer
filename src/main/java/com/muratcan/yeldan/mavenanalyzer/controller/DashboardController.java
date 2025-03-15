package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.DashboardStatsResponse;
import com.muratcan.yeldan.mavenanalyzer.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for dashboard-related endpoints.
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "API endpoints for dashboard statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard statistics including totals for projects, dependencies,
     * outdated dependencies, vulnerabilities, and recent analyses.
     *
     * @return Dashboard statistics response
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics",
            description = "Retrieve statistics for the dashboard including counts of projects, dependencies, outdated dependencies, and vulnerabilities")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        log.info("Fetching dashboard statistics");
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
} 