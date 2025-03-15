package com.muratcan.yeldan.mavenanalyzer.repository;

import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DependencyAnalysisRepository extends JpaRepository<DependencyAnalysis, Long> {

    List<DependencyAnalysis> findByProjectIdOrderByAnalysisDateDesc(Long projectId);

    DependencyAnalysis findTopByProjectIdOrderByAnalysisDateDesc(Long projectId);
} 