package com.muratcan.yeldan.mavenanalyzer.dto.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PieChartDataResponse extends ChartDataResponse {

    private List<PieChartEntry> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieChartEntry {
        private String id;
        private String label;
        private Number value;
        private String color;
    }
} 