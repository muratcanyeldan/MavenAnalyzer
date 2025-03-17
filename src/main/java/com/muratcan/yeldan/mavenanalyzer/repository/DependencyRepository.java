package com.muratcan.yeldan.mavenanalyzer.repository;

import com.muratcan.yeldan.mavenanalyzer.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long> {

    List<Dependency> findByAnalysisId(Long analysisId);

    long countByIsOutdated(Boolean isOutdated);

    Optional<Dependency> findByAnalysisIdAndGroupIdAndArtifactIdAndCurrentVersion(
            Long analysisId, String groupId, String artifactId, String currentVersion);

    @Query("SELECT COUNT(d) FROM Dependency d WHERE d.analysis.id = :analysisId AND d.isVulnerable IS NOT NULL")
    long countByAnalysisIdAndVulnerableStatusSet(@Param("analysisId") Long analysisId);
} 