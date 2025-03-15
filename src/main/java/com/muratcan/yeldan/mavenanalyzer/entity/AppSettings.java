package com.muratcan.yeldan.mavenanalyzer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // General settings
    @Column(name = "settings_key", nullable = false, unique = true)
    private String key;

    @Column(name = "settings_name")
    private String name;

    // License settings
    @Column(name = "license_checking_enabled", nullable = false)
    private Boolean licenseCheckingEnabled;

    @ElementCollection
    @CollectionTable(name = "restricted_licenses", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "license")
    private List<String> restrictedLicenses = new ArrayList<>();

    // Vulnerability settings
    @Column(name = "vulnerability_checking_enabled", nullable = false)
    private Boolean vulnerabilityCheckingEnabled;

    @Column(name = "vulnerability_check_delay")
    private Integer vulnerabilityCheckDelay;

    // Cache settings
    @Column(name = "cache_enabled", nullable = false)
    private Boolean cacheEnabled;

    @Column(name = "cache_duration_hours")
    private Integer cacheDurationHours;

    // Notification settings
    @Column(name = "show_notifications", nullable = false)
    private Boolean showNotifications;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 