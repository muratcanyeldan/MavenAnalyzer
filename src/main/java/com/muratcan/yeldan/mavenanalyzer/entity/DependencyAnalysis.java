package com.muratcan.yeldan.mavenanalyzer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dependency_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "pom_content", nullable = false, columnDefinition = "LONGTEXT")
    private String pomContent;

    @CreationTimestamp
    @Column(name = "analysis_date", updatable = false)
    private LocalDateTime analysisDate;

    @Column(name = "total_dependencies", nullable = false)
    private Integer totalDependencies;

    @Column(name = "outdated_dependencies", nullable = false)
    private Integer outdatedDependencies;

    @Column(name = "up_to_date_dependencies", nullable = false)
    private Integer upToDateDependencies;

    @Column(name = "unidentified_dependencies", nullable = false)
    private Integer unidentifiedDependencies;

    @Column(name = "chart_path")
    private String chartPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "vulnerability_check_status")
    private VulnerabilityCheckStatus vulnerabilityCheckStatus;

    @Column(name = "notify_on_completion")
    private boolean notifyOnCompletion;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Dependency> dependencies = new ArrayList<>();

    public enum VulnerabilityCheckStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
} 