// Settings.js - Updated to remove unused chart type functionality
import React, { useState, useContext, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  FormControlLabel,
  Grid,
  InputAdornment,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Switch,
  Tab,
  Tabs,
  TextField,
  Typography,
  Snackbar,
  Alert,
  CircularProgress
} from '@mui/material';
import {
  Save as SaveIcon,
  Refresh as RefreshIcon,
  Security as SecurityIcon,
  Storage as StorageIcon,
  Notifications as NotificationsIcon,
  AccountCircle as AccountCircleIcon,
  Settings as SettingsIcon,
  CloudDownload as CloudDownloadIcon,
} from '@mui/icons-material';
import { ColorModeContext } from '../App';
import api from '../services/api';

// Tab panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`settings-tabpanel-${index}`}
      aria-labelledby={`settings-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

// Function to load settings from API
const loadSettingsFromAPI = async () => {
  try {
    const response = await api.settings.getSettings();
    console.log('Settings loaded from API:', response.data);
    
    // Map API response to frontend settings format
    const mappedSettings = {
      darkMode: false, // This is managed by the frontend
      showNotifications: response.data.showNotifications !== undefined ? response.data.showNotifications : true,
      
      // Map backend settings to frontend format
      licenseChecking: response.data.licenseCheckingEnabled,
      restrictedLicenses: response.data.restrictedLicenses || ['GPL', 'AGPL'],
      vulnerabilityChecking: response.data.vulnerabilityCheckingEnabled,
      vulnerabilityCheckDelay: response.data.vulnerabilityCheckDelay || 0,
      cacheEnabled: response.data.cacheEnabled,
      cacheDuration: response.data.cacheDurationHours || 24,
      
      // Default values for settings not provided by the API
      mavenPath: '/usr/bin/mvn',
      mavenRepoPath: '~/.m2/repository',
    };
    
    return mappedSettings;
  } catch (error) {
    console.error('Error loading settings from API:', error);
    // If API fails, fallback to localStorage
    const saved = localStorage.getItem('mavenAnalyzerSettings');
    return saved ? JSON.parse(saved) : null;
  }
};

// Function to save settings to API
const saveSettingsToAPI = async (settings) => {
  try {
    // Map frontend settings to API format
    const apiSettings = {
      licenseCheckingEnabled: settings.licenseChecking,
      restrictedLicenses: settings.restrictedLicenses,
      vulnerabilityCheckingEnabled: settings.vulnerabilityChecking,
      vulnerabilityCheckDelay: settings.vulnerabilityCheckDelay,
      cacheEnabled: settings.cacheEnabled,
      cacheDurationHours: settings.cacheDuration,
      showNotifications: settings.showNotifications
    };
    
    console.log('Saving settings to API:', apiSettings);
    const response = await api.settings.updateSettings(apiSettings);
    
    // As a fallback, also save to localStorage
    localStorage.setItem('mavenAnalyzerSettings', JSON.stringify(settings));
    
    return response.data;
  } catch (error) {
    console.error('Error saving settings to API:', error);
    // If API fails, still update localStorage and resolve
    localStorage.setItem('mavenAnalyzerSettings', JSON.stringify(settings));
    throw error;
  }
};

const Settings = () => {
  const colorMode = useContext(ColorModeContext);
  const [tabValue, setTabValue] = useState(0);
  const [settings, setSettings] = useState({
    // General settings
    darkMode: colorMode.mode === 'dark',
    showNotifications: true,
    
    // Maven settings
    mavenPath: '/usr/bin/mvn',
    mavenRepoPath: '~/.m2/repository',
    
    // License settings
    licenseChecking: true,
    restrictedLicenses: ['GPL', 'AGPL'],
    
    // Vulnerability settings
    vulnerabilityChecking: true,
    vulnerabilityCheckDelay: 0,
    
    // Cache settings
    cacheEnabled: true,
    cacheDuration: 24,
  });
  
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Add loading state for the cache clear operation
  const [clearingCache, setClearingCache] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);

  // Load settings when component mounts
  useEffect(() => {
    loadSettingsFromAPI()
      .then(savedSettings => {
        if (savedSettings) {
          // Apply saved settings
          setSettings(prev => ({
            ...prev,
            ...savedSettings,
            // Keep dark mode in sync with app state
            darkMode: colorMode.mode === 'dark'
          }));
          
          // Apply dark mode if it differs from current app state
          if (savedSettings.darkMode !== (colorMode.mode === 'dark')) {
            colorMode.toggleColorMode();
          }
        }
      })
      .catch(error => {
        console.error('Error loading settings:', error);
        setSnackbar({
          open: true,
          message: 'Failed to load settings',
          severity: 'error'
        });
      })
      .finally(() => {
        setSavingSettings(false);
      });
  }, [colorMode]);

  // Effect to sync the dark mode state when it changes externally
  useEffect(() => {
    setSettings(prev => ({
      ...prev,
      darkMode: colorMode.mode === 'dark'
    }));
  }, [colorMode.mode]);

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  const handleSettingChange = (setting, value) => {
    setSettings({
      ...settings,
      [setting]: value
    });
    
    // Special handling for dark mode to update application theme
    if (setting === 'darkMode' && value !== (colorMode.mode === 'dark')) {
      colorMode.toggleColorMode();
    }
  };
  
  const handleSave = () => {
    // Save settings to backend API
    console.log('Save button clicked, saving settings...');
    setSavingSettings(true);
    saveSettingsToAPI(settings)
      .then((response) => {
        console.log('Settings saved successfully:', response);
        // Show success message
        setSnackbar({
          open: true,
          message: 'Settings saved successfully',
          severity: 'success'
        });
      })
      .catch(error => {
        console.error('Error saving settings:', error);
        setSnackbar({
          open: true,
          message: 'Failed to save settings: ' + (error.response?.data?.message || error.message || 'Unknown error'),
          severity: 'error'
        });
      })
      .finally(() => {
        console.log('Save operation finished');
        setSavingSettings(false);
      });
  };
  
  const handleReset = () => {
    // Reset to default values
    const defaultSettings = {
      darkMode: false,
      showNotifications: true,
      mavenPath: '/usr/bin/mvn',
      mavenRepoPath: '~/.m2/repository',
      licenseChecking: true,
      restrictedLicenses: ['GPL', 'AGPL'],
      vulnerabilityChecking: true,
      vulnerabilityCheckDelay: 0,
      cacheEnabled: true,
      cacheDuration: 24,
    };
    
    setSettings(defaultSettings);
    
    // If dark mode is currently enabled, switch to light mode
    if (colorMode.mode === 'dark') {
      colorMode.toggleColorMode();
    }
    
    // Show info message
    setSnackbar({
      open: true,
      message: 'Settings reset to defaults',
      severity: 'info'
    });
  };
  
  const handleCloseSnackbar = () => {
    setSnackbar({
      ...snackbar,
      open: false
    });
  };

  const handleClearCache = async () => {
    try {
      setClearingCache(true);
      
      // Determine which cache to clear based on what's enabled in the settings
      await api.cache.clearAll();
      
      setSnackbar({
        open: true,
        message: 'Cache cleared successfully',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error clearing cache:', error);
      
      setSnackbar({
        open: true,
        message: `Failed to clear cache: ${error.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setClearingCache(false);
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <SettingsIcon sx={{ mr: 1 }} />
        <Typography variant="h4" component="h1">
          Settings
        </Typography>
      </Box>
      
      <Paper sx={{ mb: 4 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={tabValue}
            onChange={handleTabChange}
            aria-label="settings tabs"
            variant="scrollable"
            scrollButtons="auto"
          >
            <Tab label="General" icon={<SettingsIcon />} iconPosition="start" />
            <Tab label="Maven Configuration" icon={<StorageIcon />} iconPosition="start" />
            <Tab label="License Checking" icon={<AccountCircleIcon />} iconPosition="start" />
            <Tab label="Vulnerability Scanning" icon={<SecurityIcon />} iconPosition="start" />
            <Tab label="Updates & Cache" icon={<CloudDownloadIcon />} iconPosition="start" />
          </Tabs>
        </Box>
        
        {/* General Settings */}
        <TabPanel value={tabValue} index={0}>
          <Typography variant="h6" gutterBottom>
            Application Settings
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <List>
                    <ListItem>
                      <ListItemIcon>
                        <SettingsIcon />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Dark Mode" 
                        secondary="Enable dark theme for the application" 
                      />
                      <Switch
                        edge="end"
                        checked={settings.darkMode}
                        onChange={(e) => handleSettingChange('darkMode', e.target.checked)}
                      />
                    </ListItem>
                    <Divider />
                    <ListItem>
                      <ListItemIcon>
                        <NotificationsIcon />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Notifications" 
                        secondary="Show notifications for completed analyses" 
                      />
                      <Switch
                        edge="end"
                        checked={settings.showNotifications}
                        onChange={(e) => handleSettingChange('showNotifications', e.target.checked)}
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        {/* Maven Configuration */}
        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Maven Configuration
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Box sx={{ mb: 3 }}>
                    <TextField
                      fullWidth
                      label="Maven Path"
                      variant="outlined"
                      value={settings.mavenPath}
                      onChange={(e) => handleSettingChange('mavenPath', e.target.value)}
                      helperText="Path to Maven executable (mvn)"
                      margin="normal"
                    />
                    
                    <TextField
                      fullWidth
                      label="Maven Repository Path"
                      variant="outlined"
                      value={settings.mavenRepoPath}
                      onChange={(e) => handleSettingChange('mavenRepoPath', e.target.value)}
                      helperText="Path to local Maven repository"
                      margin="normal"
                    />
                  </Box>
                  
                  <Alert severity="info" sx={{ mt: 2 }}>
                    Configure these settings only if you want to use a custom Maven installation.
                    If left blank, the system will use the default Maven installation.
                  </Alert>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        {/* License Settings */}
        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" gutterBottom>
            License Checking Configuration
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Box sx={{ mb: 3 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={settings.licenseChecking}
                          onChange={(e) => handleSettingChange('licenseChecking', e.target.checked)}
                        />
                      }
                      label="Enable License Checking"
                    />
                    
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
                      When enabled, the system will check dependency licenses and flag potentially problematic licenses.
                    </Typography>
                    
                    <TextField
                      fullWidth
                      label="Restricted Licenses"
                      variant="outlined"
                      value={settings.restrictedLicenses.join(', ')}
                      onChange={(e) => handleSettingChange('restrictedLicenses', e.target.value.split(',').map(item => item.trim()))}
                      helperText="Comma-separated list of license types to flag as restricted"
                      margin="normal"
                      disabled={!settings.licenseChecking}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        {/* Vulnerability Settings */}
        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" gutterBottom>
            Vulnerability Scanning Configuration
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Box sx={{ mb: 3 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={settings.vulnerabilityChecking}
                          onChange={(e) => handleSettingChange('vulnerabilityChecking', e.target.checked)}
                        />
                      }
                      label="Enable Vulnerability Scanning"
                    />
                    
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
                      When enabled, the system will check for known vulnerabilities in dependencies.
                    </Typography>
                    
                    <TextField
                      fullWidth
                      label="Vulnerability Check Delay (seconds)"
                      variant="outlined"
                      type="number"
                      value={settings.vulnerabilityCheckDelay}
                      onChange={(e) => handleSettingChange('vulnerabilityCheckDelay', parseInt(e.target.value))}
                      helperText="Delay between vulnerability checks to avoid API rate limits (0 for no delay)"
                      margin="normal"
                      disabled={!settings.vulnerabilityChecking}
                      InputProps={{
                        endAdornment: <InputAdornment position="end">seconds</InputAdornment>,
                      }}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        {/* Updates & Cache Settings */}
        <TabPanel value={tabValue} index={4}>
          <Typography variant="h6" gutterBottom>
            Updates & Cache Settings
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Box sx={{ mb: 3 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={settings.cacheEnabled}
                          onChange={(e) => handleSettingChange('cacheEnabled', e.target.checked)}
                        />
                      }
                      label="Enable Cache"
                    />
                    
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
                      Cache Maven Central responses to improve performance and reduce API calls
                    </Typography>
                    
                    <TextField
                      fullWidth
                      label="Cache Duration"
                      variant="outlined"
                      type="number"
                      value={settings.cacheDuration}
                      onChange={(e) => handleSettingChange('cacheDuration', parseInt(e.target.value))}
                      helperText="How long to keep cached data (in hours)"
                      margin="normal"
                      disabled={!settings.cacheEnabled}
                      InputProps={{
                        endAdornment: <InputAdornment position="end">hours</InputAdornment>,
                      }}
                    />
                    
                    <Box sx={{ mt: 2 }}>
                      <Button 
                        variant="outlined" 
                        startIcon={<RefreshIcon />}
                        onClick={handleClearCache}
                        disabled={!settings.cacheEnabled || clearingCache}
                      >
                        {clearingCache ? 'Clearing Cache...' : 'Clear Cache'}
                      </Button>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        <Box sx={{ p: 3, display: 'flex', justifyContent: 'flex-end' }}>
          <Button 
            variant="outlined" 
            startIcon={<RefreshIcon />} 
            onClick={handleReset}
            sx={{ mr: 2 }}
            disabled={savingSettings}
          >
            Reset to Defaults
          </Button>
          <Button 
            variant="contained" 
            startIcon={savingSettings ? null : <SaveIcon />}
            onClick={handleSave}
            disabled={savingSettings}
            data-testid="save-settings-button"
          >
            {savingSettings ? (
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <CircularProgress size={24} color="inherit" sx={{ mr: 1 }} />
                Saving...
              </Box>
            ) : (
              'Save Settings'
            )}
          </Button>
        </Box>
      </Paper>
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default Settings; 