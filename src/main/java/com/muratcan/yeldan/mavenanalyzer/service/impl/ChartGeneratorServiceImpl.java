package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ChartGeneratorServiceImpl implements ChartGeneratorService {

    // Constants for dependency status
    private static final String STATUS_UP_TO_DATE = "Up to Date";
    private static final String STATUS_OUTDATED = "Outdated";
    private static final String STATUS_UNIDENTIFIED = "Unidentified";
    // Constants for chart types
    private static final String CHART_TYPE_PIE = "Pie Chart";
    private static final String CHART_TYPE_BAR = "Bar Chart";
    private static final String CHART_TYPE_LINE = "Line Chart";
    // Constants for chart series labels
    private static final String SERIES_PREVIOUS = "Previous";
    private static final String SERIES_CURRENT = "Current";
    // Constants for error messages
    private static final String ERROR_TITLE = "Error";
    private static final String ERROR_GENERATE_CHART_PREFIX = "Failed to generate chart: ";
    @Value("${chart.output.directory}")
    private String chartOutputDirectory;

    @Override
    public ChartResponse generateDependencyStatusChart(DependencyAnalysis analysis) {
        log.debug("Generating dependency status chart for analysis ID: {}", analysis.getId());

        String fileName = "dependency-status-" + analysis.getId() + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + File.separator + fileName;

        try {
            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();

            if (analysis.getUpToDateDependencies() > 0) {
                dataset.setValue(STATUS_UP_TO_DATE, analysis.getUpToDateDependencies());
            }

            if (analysis.getOutdatedDependencies() > 0) {
                dataset.setValue(STATUS_OUTDATED, analysis.getOutdatedDependencies());
            }

            if (analysis.getUnidentifiedDependencies() > 0) {
                dataset.setValue(STATUS_UNIDENTIFIED, analysis.getUnidentifiedDependencies());
            }

            JFreeChart chart = ChartFactory.createPieChart(
                    "Dependency Status", // chart title
                    dataset,             // data
                    true,                // include legend
                    true,                // tooltips
                    false                // URLs
            );

            PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
            plot.setSectionPaint(STATUS_UP_TO_DATE, new Color(46, 204, 113));
            plot.setSectionPaint(STATUS_OUTDATED, new Color(231, 76, 60));
            plot.setSectionPaint(STATUS_UNIDENTIFIED, new Color(149, 165, 166));

            ChartUtils.saveChartAsPNG(new File(filePath), chart, 600, 400);

            log.info("Dependency status chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType(CHART_TYPE_PIE)
                    .title("Dependency Status")
                    .description("Distribution of dependency statuses in the project")
                    .build();

        } catch (IOException e) {
            log.error("Error generating dependency status chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType(CHART_TYPE_PIE)
                    .title(ERROR_TITLE)
                    .description(ERROR_GENERATE_CHART_PREFIX + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChartResponse generateVulnerabilityChart(DependencyAnalysis analysis) {
        log.debug("Generating vulnerability chart for analysis ID: {}", analysis.getId());

        String fileName = "vulnerability-" + analysis.getId() + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + File.separator + fileName;

        try {
            long vulnerableDependencies = analysis.getDependencies().stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsVulnerable()))
                    .count();

            long safeDependencies = analysis.getDependencies().size() - vulnerableDependencies;

            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            dataset.setValue("Vulnerable", vulnerableDependencies);
            dataset.setValue("Safe", safeDependencies);

            JFreeChart chart = ChartFactory.createPieChart(
                    "Vulnerability Status",  // chart title
                    dataset,                // data
                    false,                  // include legend (set to false to remove legend)
                    true,                   // tooltips
                    false                   // URLs
            );

            PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
            plot.setSectionPaint("Vulnerable", new Color(231, 76, 60));   // Red for vulnerable
            plot.setSectionPaint("Safe", new Color(46, 204, 113));       // Green for safe

            ChartUtils.saveChartAsPNG(new File(filePath), chart, 600, 400);

            log.info("Vulnerability chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType(CHART_TYPE_PIE)
                    .title("Vulnerability Status")
                    .description("Distribution of vulnerable and safe dependencies")
                    .build();

        } catch (IOException e) {
            log.error("Error generating vulnerability chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType(CHART_TYPE_PIE)
                    .title(ERROR_TITLE)
                    .description(ERROR_GENERATE_CHART_PREFIX + e.getMessage())
                    .build();
        }
    }

    private String ensureDirectoryExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Error creating chart directory", e);
            return System.getProperty("java.io.tmpdir");
        }
    }

    private String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                "-" + UUID.randomUUID().toString().substring(0, 8);
    }
} 