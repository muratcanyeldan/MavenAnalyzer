-- Create app_settings table
CREATE TABLE app_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settings_key VARCHAR(50) NOT NULL UNIQUE,
    settings_name VARCHAR(100),
    license_checking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vulnerability_checking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    vulnerability_check_delay INT DEFAULT 0,
    cache_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cache_duration_hours INT DEFAULT 24,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create restricted_licenses table (for @ElementCollection)
CREATE TABLE restricted_licenses (
    settings_id BIGINT NOT NULL,
    license VARCHAR(100) NOT NULL,
    PRIMARY KEY (settings_id, license),
    FOREIGN KEY (settings_id) REFERENCES app_settings(id) ON DELETE CASCADE
);

-- Insert default app settings
INSERT INTO app_settings (
    settings_key, 
    settings_name, 
    license_checking_enabled, 
    vulnerability_checking_enabled, 
    vulnerability_check_delay, 
    cache_enabled, 
    cache_duration_hours
) VALUES (
    'application', 
    'Application Settings', 
    TRUE, 
    TRUE, 
    0, 
    TRUE, 
    24
);

-- Insert default restricted licenses
INSERT INTO restricted_licenses (settings_id, license) 
SELECT id, 'GPL' FROM app_settings WHERE settings_key = 'application';

INSERT INTO restricted_licenses (settings_id, license) 
SELECT id, 'AGPL' FROM app_settings WHERE settings_key = 'application'; 