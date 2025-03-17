package com.muratcan.yeldan.mavenanalyzer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dependencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DependencyAnalysis analysis;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "artifact_id", nullable = false)
    private String artifactId;

    @Column(name = "current_version", nullable = false)
    private String currentVersion;

    @Column(name = "latest_version")
    private String latestVersion;

    @Column(name = "is_outdated")
    private Boolean isOutdated;

    @Column(name = "is_vulnerable")
    private Boolean isVulnerable;

    @Column(name = "vulnerable_count")
    private Integer vulnerableCount;

    @Column(name = "scope")
    private String scope;

    @Column(name = "license")
    private String license;

    @Column(name = "status")
    private String status;

    @Column(name = "is_bom_managed")
    private Boolean isBomManaged;

    @Column(name = "estimated_version")
    private String estimatedVersion;

    @OneToMany(mappedBy = "dependency", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
} 