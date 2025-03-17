package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ReportResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.Dependency;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.entity.Vulnerability;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyAnalysisRepository;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import com.muratcan.yeldan.mavenanalyzer.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final String LICENSE_UNKNOWN = "Unknown";
    private static final String ANALYSIS_NOT_FOUND_WITH_ID = "Analysis not found with ID: ";
    private static final String STATUS_VULNERABLE = "Vulnerable";
    private static final String STATUS_OUTDATED = "Outdated";
    private static final String STATUS_BOM_MANAGED = "BOM Managed";
    private static final String STATUS_UP_TO_DATE = "Up to date";
    private final DependencyAnalysisRepository dependencyAnalysisRepository;
    private final ChartGeneratorService chartGeneratorService;
    private PdfFont boldFont;

    @Value("${report.output.directory}")
    private String reportOutputDirectory;

    @Override
    public ReportResponse generateFullReport(Long analysisId) {
        log.info("Generating full report for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = dependencyAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_WITH_ID + analysisId));

        File outputDir = new File(reportOutputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String fileName = String.format("dependency-report-%s-%s.pdf",
                analysis.getProject().getName().replaceAll("[^a-zA-Z0-9]", "-"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        String filePath = reportOutputDirectory + File.separator + fileName;

        try {
            generatePdfReport(analysis, filePath);

            return ReportResponse.builder()
                    .reportPath(fileName)
                    .fileName(fileName)
                    .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .projectName(analysis.getProject().getName())
                    .analysisDate(analysis.getAnalysisDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .totalDependencies(analysis.getTotalDependencies())
                    .outdatedDependencies(analysis.getOutdatedDependencies())
                    .vulnerableDependencies(countVulnerableDependencies(analysis.getDependencies()))
                    .build();

        } catch (IOException e) {
            log.error("Error generating PDF report: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void generatePdfReport(DependencyAnalysis analysis, String filePath) throws IOException {
        File outputDir = new File(reportOutputDirectory);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        try {
            boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        } catch (IOException e) {
            log.error("Error creating fonts for PDF", e);
        }

        Paragraph title = new Paragraph("Maven Dependency Analysis Report")
                .setFontSize(24);
        if (boldFont != null) {
            title.setFont(boldFont);
        }
        document.add(title);

        addProjectInfo(document, analysis);
        addSummarySection(document, analysis);
        addChartsSection(document, analysis);
        addDependencyDetails(document, analysis.getDependencies());

        if (analysis.getVulnerabilityCheckStatus() == DependencyAnalysis.VulnerabilityCheckStatus.COMPLETED) {
            addVulnerabilitySection(document, analysis.getDependencies());
        }
    }

    private void addProjectInfo(Document document, DependencyAnalysis analysis) {
        Paragraph header = new Paragraph("\nProject Information").setFontSize(18);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        Table table = new Table(2).useAllAvailableWidth();

        addTableRow(table, "Project Name", analysis.getProject().getName());
        addTableRow(table, "Analysis Date", analysis.getAnalysisDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        addTableRow(table, "Total Dependencies", String.valueOf(analysis.getTotalDependencies()));
        addTableRow(table, "Outdated Dependencies", String.valueOf(analysis.getOutdatedDependencies()));
        addTableRow(table, "Up-to-date Dependencies", String.valueOf(analysis.getUpToDateDependencies()));

        document.add(table);
    }

    private void addSummarySection(Document document, DependencyAnalysis analysis) {
        Paragraph header = new Paragraph("\nSummary").setFontSize(18);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        List<Dependency> dependencies = analysis.getDependencies();
        int vulnerableCount = countVulnerableDependencies(dependencies);
        int bomManagedCount = countBomManagedDependencies(dependencies);

        Table table = new Table(2).useAllAvailableWidth();
        addTableRow(table, "Total Dependencies", String.valueOf(dependencies.size()));
        addTableRow(table, "Outdated Dependencies", String.valueOf(analysis.getOutdatedDependencies()));
        addTableRow(table, "Vulnerable Dependencies", String.valueOf(vulnerableCount));
        addTableRow(table, "BOM Managed Dependencies", String.valueOf(bomManagedCount));

        document.add(table);
    }

    private void addDependencyDetails(Document document, List<Dependency> dependencies) {
        Paragraph header = new Paragraph("\nDependency Details").setFontSize(18);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 15, 15, 15, 15, 20}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("Group ID"));
        table.addHeaderCell(createHeaderCell("Artifact ID"));
        table.addHeaderCell(createHeaderCell("Current Version"));
        table.addHeaderCell(createHeaderCell("Latest Version"));
        table.addHeaderCell(createHeaderCell("Status"));
        table.addHeaderCell(createHeaderCell("License"));

        for (Dependency dep : dependencies) {
            table.addCell(dep.getGroupId());
            table.addCell(dep.getArtifactId());
            table.addCell(dep.getCurrentVersion());
            table.addCell(dep.getLatestVersion() != null ? dep.getLatestVersion() : "N/A");
            table.addCell(getStatusText(dep));
            table.addCell(dep.getLicense() != null ? dep.getLicense() : LICENSE_UNKNOWN);
        }

        document.add(table);
    }

    private void addVulnerabilitySection(Document document, List<Dependency> dependencies) {
        Paragraph header = new Paragraph("\nVulnerability Analysis").setFontSize(18);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 15, 40, 20}))
                .useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("Dependency"));
        table.addHeaderCell(createHeaderCell("Severity"));
        table.addHeaderCell(createHeaderCell("Description"));
        table.addHeaderCell(createHeaderCell("Fixed Version"));

        for (Dependency dep : dependencies) {
            if (Boolean.TRUE.equals(dep.getIsVulnerable())) {
                for (Vulnerability vuln : dep.getVulnerabilities()) {
                    table.addCell(String.format("%s:%s", dep.getGroupId(), dep.getArtifactId()));
                    table.addCell(vuln.getSeverity());
                    table.addCell(vuln.getDescription());
                    table.addCell(vuln.getFixedInVersion() != null ? vuln.getFixedInVersion() : "N/A");
                }
            }
        }

        document.add(table);
    }

    private void addChartsSection(Document document, DependencyAnalysis analysis) {
        Paragraph header = new Paragraph("\nCharts and Visualizations").setFontSize(18);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        try {
            ChartResponse dependencyStatusChart = chartGeneratorService.generateDependencyStatusChart(analysis);
            if (dependencyStatusChart.getChartPath() != null) {
                addChartToDocument(document, dependencyStatusChart, "Dependency Status Distribution");
            }

            if (analysis.getVulnerabilityCheckStatus() == DependencyAnalysis.VulnerabilityCheckStatus.COMPLETED) {
                ChartResponse vulnerabilityChart = chartGeneratorService.generateVulnerabilityChart(analysis);
                if (vulnerabilityChart.getChartPath() != null) {
                    addChartToDocument(document, vulnerabilityChart, "Vulnerability Status Distribution");
                }
            }

        } catch (IOException e) {
            log.error("Error adding charts to PDF report", e);
            document.add(new Paragraph("Error generating charts")
                    .setFontColor(ColorConstants.RED));
        }
    }

    private void addChartToDocument(Document document, ChartResponse chartResponse, String title) throws IOException {
        Paragraph header = new Paragraph(title).setFontSize(14);
        if (boldFont != null) {
            header.setFont(boldFont);
        }
        document.add(header);

        Image chartImage = new Image(ImageDataFactory.create(chartResponse.getChartPath()));
        chartImage.setWidth(UnitValue.createPercentValue(80))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        document.add(chartImage);

        document.add(new Paragraph("\n"));
    }

    private Cell createHeaderCell(String text) {
        Cell cell = new Cell();
        Paragraph p = new Paragraph(text);
        if (boldFont != null) {
            p.setFont(boldFont);
        }
        cell.add(p);
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        return cell;
    }

    private void addTableRow(Table table, String label, String value) {
        Paragraph labelPara = new Paragraph(label);
        if (boldFont != null) {
            labelPara.setFont(boldFont);
        }
        table.addCell(new Cell().add(labelPara));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private String getStatusText(Dependency dep) {
        if (Boolean.TRUE.equals(dep.getIsVulnerable())) {
            return STATUS_VULNERABLE;
        } else if (Boolean.TRUE.equals(dep.getIsOutdated())) {
            return STATUS_OUTDATED;
        } else if (Boolean.TRUE.equals(dep.getIsBomManaged())) {
            return STATUS_BOM_MANAGED;
        } else {
            return STATUS_UP_TO_DATE;
        }
    }

    private int countVulnerableDependencies(List<Dependency> dependencies) {
        return (int) dependencies.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsVulnerable()))
                .count();
    }

    private int countBomManagedDependencies(List<Dependency> dependencies) {
        return (int) dependencies.stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsBomManaged()))
                .count();
    }
} 