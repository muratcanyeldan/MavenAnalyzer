package com.muratcan.yeldan.mavenanalyzer.repository;

import com.muratcan.yeldan.mavenanalyzer.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {

    Optional<AppSettings> findByKey(String key);
} 