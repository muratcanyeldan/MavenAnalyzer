package com.muratcan.yeldan.mavenanalyzer.dto.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base response class for all chart data
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponse {

    private String chartType;
    private String title;
    private String description;
    private String summary;
} 