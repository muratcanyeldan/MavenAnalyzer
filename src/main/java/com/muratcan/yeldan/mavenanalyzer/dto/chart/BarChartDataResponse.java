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
public class BarChartDataResponse extends ChartDataResponse {

    private List<BarChartEntry> data;
    private List<String> keys;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarChartEntry {
        private String category;
        private Number count;
        private String color;
    }
} 