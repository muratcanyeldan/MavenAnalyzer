package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
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
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ChartGeneratorServiceImpl implements ChartGeneratorService {

    @Value("${chart.output.directory}")
    private String chartOutputDirectory;

    @Override
    public ChartResponse generateDependencyStatusChart(DependencyAnalysis analysis) {
        log.debug("Generating dependency status chart for analysis ID: {}", analysis.getId());

        String fileName = "dependency-status-" + analysis.getId() + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + "/" + fileName;

        try {
            // Create dataset for the pie chart
            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();

            if (analysis.getUpToDateDependencies() > 0) {
                dataset.setValue("Up to Date", analysis.getUpToDateDependencies());
            }

            if (analysis.getOutdatedDependencies() > 0) {
                dataset.setValue("Outdated", analysis.getOutdatedDependencies());
            }

            if (analysis.getUnidentifiedDependencies() > 0) {
                dataset.setValue("Unidentified", analysis.getUnidentifiedDependencies());
            }

            // Create the chart
            JFreeChart chart = ChartFactory.createPieChart(
                    "Dependency Status", // chart title
                    dataset,             // data
                    true,                // include legend
                    true,                // tooltips
                    false                // URLs
            );

            // Customize the chart
            PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
            plot.setSectionPaint("Up to Date", new Color(46, 204, 113));
            plot.setSectionPaint("Outdated", new Color(231, 76, 60));
            plot.setSectionPaint("Unidentified", new Color(149, 165, 166));

            // Save the chart to a file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 600, 400);

            log.info("Dependency status chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType("Pie Chart")
                    .title("Dependency Status")
                    .description("Distribution of dependency statuses in the project")
                    .build();

        } catch (IOException e) {
            log.error("Error generating dependency status chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType("Pie Chart")
                    .title("Error")
                    .description("Failed to generate chart: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChartResponse generateComparisonChart(DependencyAnalysis previousAnalysis, DependencyAnalysis currentAnalysis) {
        log.debug("Generating comparison chart between analyses: {} and {}",
                previousAnalysis.getId(), currentAnalysis.getId());

        String fileName = "comparison-" + previousAnalysis.getId() + "-" + currentAnalysis.getId() + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + "/" + fileName;

        try {
            // Create dataset for the bar chart
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Add previous analysis data
            dataset.addValue(previousAnalysis.getUpToDateDependencies(), "Previous", "Up to Date");
            dataset.addValue(previousAnalysis.getOutdatedDependencies(), "Previous", "Outdated");
            dataset.addValue(previousAnalysis.getUnidentifiedDependencies(), "Previous", "Unidentified");

            // Add current analysis data
            dataset.addValue(currentAnalysis.getUpToDateDependencies(), "Current", "Up to Date");
            dataset.addValue(currentAnalysis.getOutdatedDependencies(), "Current", "Outdated");
            dataset.addValue(currentAnalysis.getUnidentifiedDependencies(), "Current", "Unidentified");

            // Create the chart
            JFreeChart chart = ChartFactory.createBarChart(
                    "Dependency Status Comparison",  // chart title
                    "Status",                      // domain axis label
                    "Count",                       // range axis label
                    dataset,                       // data
                    PlotOrientation.VERTICAL,      // orientation
                    true,                          // include legend
                    true,                          // tooltips
                    false                          // URLs
            );

            // Customize the chart
            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();

            renderer.setSeriesPaint(0, new Color(52, 152, 219));  // Previous in blue
            renderer.setSeriesPaint(1, new Color(46, 204, 113));  // Current in green

            // Save the chart to a file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 500);

            log.info("Comparison chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType("Bar Chart")
                    .title("Dependency Status Comparison")
                    .description("Comparison of dependency statuses between analyses")
                    .build();

        } catch (IOException e) {
            log.error("Error generating comparison chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType("Bar Chart")
                    .title("Error")
                    .description("Failed to generate chart: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChartResponse generateVulnerabilityChart(DependencyAnalysis analysis) {
        log.debug("Generating vulnerability chart for analysis ID: {}", analysis.getId());

        String fileName = "vulnerability-" + analysis.getId() + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + "/" + fileName;

        try {
            // Count vulnerabilities
            long vulnerableDependencies = analysis.getDependencies().stream()
                    .filter(d -> Boolean.TRUE.equals(d.getIsVulnerable()))
                    .count();

            long safeeDependencies = analysis.getDependencies().size() - vulnerableDependencies;

            // Create dataset for the pie chart
            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            dataset.setValue("Vulnerable", vulnerableDependencies);
            dataset.setValue("Safe", safeeDependencies);

            // Create the chart
            JFreeChart chart = ChartFactory.createPieChart(
                    "Vulnerability Status",  // chart title
                    dataset,                // data
                    false,                  // include legend (set to false to remove legend)
                    true,                   // tooltips
                    false                   // URLs
            );

            // Customize the chart
            PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
            plot.setSectionPaint("Vulnerable", new Color(231, 76, 60));   // Red for vulnerable
            plot.setSectionPaint("Safe", new Color(46, 204, 113));       // Green for safe

            // Save the chart to a file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 600, 400);

            log.info("Vulnerability chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType("Pie Chart")
                    .title("Vulnerability Status")
                    .description("Distribution of vulnerable and safe dependencies")
                    .build();

        } catch (IOException e) {
            log.error("Error generating vulnerability chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType("Pie Chart")
                    .title("Error")
                    .description("Failed to generate chart: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ChartResponse generateHistoryTrendChart(List<DependencyAnalysis> analyses) {
        if (analyses.isEmpty()) {
            log.warn("Cannot generate history trend chart - no analyses provided");
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType("Line Chart")
                    .title("Error")
                    .description("No analyses available for trend chart")
                    .build();
        }

        Long projectId = analyses.get(0).getProject().getId();
        log.debug("Generating history trend chart for project ID: {}", projectId);

        String fileName = "history-trend-" + projectId + "-" + generateTimestamp() + ".png";
        String filePath = ensureDirectoryExists(chartOutputDirectory) + "/" + fileName;

        try {
            // Create dataset for the line chart
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Add data for each analysis - we'll display them in reverse chronological order (latest first)
            for (int i = analyses.size() - 1; i >= 0; i--) {
                DependencyAnalysis analysis = analyses.get(i);
                String date = analysis.getAnalysisDate().format(DateTimeFormatter.ofPattern("MM/dd"));

                dataset.addValue(analysis.getUpToDateDependencies(), "Up to Date", date);
                dataset.addValue(analysis.getOutdatedDependencies(), "Outdated", date);
                dataset.addValue(analysis.getUnidentifiedDependencies(), "Unidentified", date);
            }

            // Create the chart
            JFreeChart chart = ChartFactory.createLineChart(
                    "Dependency Status History",  // chart title
                    "Analysis Date",            // domain axis label
                    "Count",                    // range axis label
                    dataset,                    // data
                    PlotOrientation.VERTICAL,   // orientation
                    true,                       // include legend
                    true,                       // tooltips
                    false                       // URLs
            );

            // Save the chart to a file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 500);

            log.info("History trend chart generated at: {}", filePath);

            return ChartResponse.builder()
                    .chartPath(filePath)
                    .chartType("Line Chart")
                    .title("Dependency Status History")
                    .description("Historical trend of dependency statuses over time")
                    .build();

        } catch (IOException e) {
            log.error("Error generating history trend chart", e);
            return ChartResponse.builder()
                    .chartPath(null)
                    .chartType("Line Chart")
                    .title("Error")
                    .description("Failed to generate chart: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Ensure the output directory exists and return its path
     */
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

    /**
     * Generate a timestamp for unique filenames
     */
    private String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                "-" + UUID.randomUUID().toString().substring(0, 8);
    }
} 