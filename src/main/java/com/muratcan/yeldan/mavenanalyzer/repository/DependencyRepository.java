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

    List<Dependency> findByAnalysisIdAndIsOutdated(Long analysisId, Boolean isOutdated);

    List<Dependency> findByAnalysisIdAndIsVulnerable(Long analysisId, Boolean isVulnerable);

    long countByIsOutdated(Boolean isOutdated);

    /**
     * Find a dependency by its unique combination of analysis ID, groupId, artifactId, and version
     */
    Optional<Dependency> findByAnalysisIdAndGroupIdAndArtifactIdAndCurrentVersion(
            Long analysisId, String groupId, String artifactId, String currentVersion);

    /**
     * Count dependencies for an analysis that have had their vulnerability status set
     * This helps track progress of vulnerability scanning
     */
    @Query("SELECT COUNT(d) FROM Dependency d WHERE d.analysis.id = :analysisId AND d.isVulnerable IS NOT NULL")
    long countByAnalysisIdAndVulnerableStatusSet(@Param("analysisId") Long analysisId);
} 