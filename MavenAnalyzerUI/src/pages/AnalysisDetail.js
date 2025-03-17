import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import {
  Typography,
  Paper,
  Box,
  Grid,
  Tabs,
  Tab,
  Card,
  CardContent,
  CircularProgress,
  Button,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  TextField,
  InputAdornment,
  Snackbar,
  Alert,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Download as DownloadIcon,
  Search as SearchIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import api from '../services/api';
import { DependencyStatusChart, VulnerabilitySeverityChart, LicenseDistributionChart, VulnerabilityByDependencyChart } from '../components/charts';
import BomManagedVersion from '../components/BomManagedVersion';
import { toast } from 'react-hot-toast';

// Tab panel component for tab content
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`analysis-tabpanel-${index}`}
      aria-labelledby={`analysis-tab-${index}`}
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

const AnalysisDetail = () => {
  const params = useParams();
  const analysisId = params.id;
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [vulnerabilitiesLoading, setVulnerabilitiesLoading] = useState(true);
  const [analysis, setAnalysis] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [dependencies, setDependencies] = useState([]);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });
  const [charts, setCharts] = useState({
    dependencyUpdates: null,
    vulnerabilities: null,
    licenses: null
  });
  const [chartsLoading, setChartsLoading] = useState(false);
  const [reportGenerating, setReportGenerating] = useState(false);
  const { enqueueSnackbar } = useSnackbar();
  const [restrictedLicenses, setRestrictedLicenses] = useState(['GPL', 'AGPL', 'LGPL', 'UNKNOWN']);
  const [shouldRefreshCharts, setShouldRefreshCharts] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [previousVulnerabilityStatus, setPreviousVulnerabilityStatus] = useState(null);

  // Function to fetch chart data
  const fetchCharts = useCallback(async () => {
    setChartsLoading(true);
    try {
      console.log(`Fetching chart data for analysis ID: ${analysisId}`);
      
      const [
        dependencyStatusResponse, 
        vulnerabilityStatusResponse, 
        vulnerabilitySeverityResponse,
        licenseDistributionResponse
      ] = await Promise.all([
        api.charts.getDependencyStatusData(analysisId),
        api.charts.getVulnerabilityStatusData(analysisId),
        api.charts.getVulnerabilitySeverityData(analysisId),
        api.charts.getLicenseDistributionData(analysisId)
      ]);
      
      setCharts({
        dependencyUpdates: dependencyStatusResponse.data,
        vulnerabilityStatus: vulnerabilityStatusResponse.data,
        vulnerabilitySeverity: vulnerabilitySeverityResponse.data,
        licenses: licenseDistributionResponse.data
      });
      
      console.log('Chart data loaded successfully');
      setShouldRefreshCharts(false);
    } catch (error) {
      console.error('Error fetching chart data:', error);
      
      // Fallback to using the analysis data directly
      setCharts({
        dependencyUpdates: analysis,
        vulnerabilityStatus: analysis,
        vulnerabilitySeverity: analysis,
        licenses: analysis
      });
      
      // Show error notification
      setSnackbar({
        open: true,
        message: 'Error loading chart data: ' + (error.message || 'Unknown error'),
        severity: 'warning'
      });
      setShouldRefreshCharts(false);
    } finally {
      setChartsLoading(false);
    }
  }, [analysisId, analysis, setCharts, setChartsLoading, setSnackbar]);

  // Poll for vulnerability updates
  const pollForVulnerabilityUpdates = useCallback(async () => {
    try {
      console.log(`Starting vulnerability polling for analysis ID: ${analysisId}`);
      
      // Set an interval to check for vulnerability updates
      const intervalId = setInterval(async () => {
        try {
          // Use the specific vulnerability status endpoint for more efficient polling
          console.log(`Polling vulnerability status for analysis ID: ${analysisId}`);
          const response = await api.dependencyAnalysis.getVulnerabilityStatus(analysisId);
          
          console.log(`Received vulnerability status for analysis ID: ${analysisId}`, {
            status: response.data.vulnerabilityCheckStatus,
            dependencies: response.data.dependencies?.length || 0
          });
          
          // Calculate the progress percentage for the UI
          const totalDeps = response.data.dependencies?.length || 0;
          const processedDeps = response.data.dependencies?.filter(d => d.vulnerableCount !== null).length || 0;
          const progressPercent = totalDeps > 0 ? Math.round((processedDeps / totalDeps) * 100) : 0;
          
          // Update the analysis with the latest data
          const totalVulnerabilities = response.data.dependencies 
            ? response.data.dependencies.reduce((total, dep) => total + (dep.vulnerableCount || 0), 0)
            : 0;
            
          setAnalysis({
            ...response.data,
            vulnerableCount: totalVulnerabilities,
            scanProgress: progressPercent,
            processedDependencies: processedDeps,
            totalDependencies: totalDeps,
            status: response.data.vulnerabilityCheckStatus === 'IN_PROGRESS' ? 'in_progress' : 
                   response.data.vulnerabilityCheckStatus === 'COMPLETED' ? 'completed' :
                   response.data.vulnerabilityCheckStatus === 'FAILED' ? 'failed' :
                   response.data.status || 'completed'
          });
          
          if (response.data.dependencies) {
            setDependencies(response.data.dependencies);
          }
          
          // If vulnerability checking is complete, stop polling
          if (response.data.vulnerabilityCheckStatus !== 'IN_PROGRESS') {
            console.log('Vulnerability check completed - state update will trigger chart refresh');
            clearInterval(intervalId);
            setVulnerabilitiesLoading(false);
            
            // Show notification when status changes to completed
            setSnackbar({
              open: true,
              message: `Analysis status updated: ${response.data.vulnerabilityCheckStatus === 'COMPLETED' ? 'Completed' : response.data.vulnerabilityCheckStatus}`,
              severity: response.data.vulnerabilityCheckStatus === 'COMPLETED' ? 'success' : 'info'
            });
            
            // No need to call fetchCharts directly - use the dedicated state variable
            console.log('Setting shouldRefreshCharts to trigger chart refresh');
            setShouldRefreshCharts(true);
          }
        } catch (pollingError) {
          console.error('Error during polling interval:', pollingError);
          
          // Show a warning but don't stop polling yet
          setSnackbar({
            open: true,
            message: 'Error checking vulnerability status. Retrying...',
            severity: 'warning'
          });
        }
      }, 3000); // Check every 3 seconds
      
      // Clean up the interval when the component unmounts
      return () => {
        console.log(`Cleaning up polling interval for analysis ID: ${analysisId}`);
        clearInterval(intervalId);
      };
    } catch (error) {
      console.error('Error setting up vulnerability polling:', error);
      setVulnerabilitiesLoading(false);
      setSnackbar({
        open: true,
        message: 'Could not set up vulnerability status monitoring. Try refreshing the page.',
        severity: 'error'
      });
    }
  }, [analysisId]);
  
  // Add a dedicated effect just for chart refreshes
  useEffect(() => {
    if (shouldRefreshCharts && analysisId && analysis?.id) {
      console.log('Refreshing charts due to shouldRefreshCharts flag');
      fetchCharts();
    }
  }, [shouldRefreshCharts, analysisId, analysis?.id, fetchCharts]);

  // Modified effect to fetch charts on initial load
  useEffect(() => {
    if (analysisId && analysis?.id && !shouldRefreshCharts) {
      console.log('Initial chart load for new analysis data');
      fetchCharts();
    }
  }, [analysisId, analysis?.id, fetchCharts, shouldRefreshCharts]);

  // Separate effect for vulnerability updates to avoid circular dependencies
  useEffect(() => {
    // Don't proceed if analysisId is undefined or null
    if (!analysisId) {
      return;
    }

    // Fetch analysis data
    const fetchAnalysisData = async () => {
      setLoading(true);
      try {
        // Get analysis details
        console.log(`Fetching analysis data for ID: ${analysisId}`);
        const response = await api.dependencyAnalysis.getById(analysisId);
        console.log('Analysis API response:', response.data);
        
        // Additional debugging for dependencies
        const dependencies = response.data.dependencies || [];
        console.log(`Received ${dependencies.length} dependencies from API`);
        
        // Calculate total vulnerabilities for the header stats
        const totalVulnerabilities = dependencies.reduce((total, dep) => total + (dep.vulnerableCount || 0), 0);
        
        // Combine the analysis data with calculated values
        setAnalysis({
          ...response.data,
          vulnerableCount: totalVulnerabilities,
          // Map the vulnerabilityCheckStatus to a UI-friendly status
          status: response.data.vulnerabilityCheckStatus === 'IN_PROGRESS' ? 'in_progress' : 
                 response.data.vulnerabilityCheckStatus === 'COMPLETED' ? 'completed' :
                 response.data.vulnerabilityCheckStatus === 'FAILED' ? 'failed' :
                 response.data.status || 'completed'
        });
        
        // If the analysis contains dependencies, set them
        if (dependencies.length > 0) {
          setDependencies(dependencies);
        }
        
        // If vulnerability checking is still in progress, start a polling mechanism
        if (response.data.vulnerabilityCheckStatus === 'IN_PROGRESS') {
          pollForVulnerabilityUpdates();
        } else {
          setVulnerabilitiesLoading(false);
        }

        // Check if vulnerability status changed from IN_PROGRESS to COMPLETED
        if (previousVulnerabilityStatus === 'IN_PROGRESS' && 
            response.data.vulnerabilityCheckStatus === 'COMPLETED' &&
            response.data.notifyOnCompletion) {
          
          // Show completion notification
          toast.success(`Analysis completed for ${response.data.projectName}! Found ${response.data.totalDependencies} dependencies (${response.data.outdatedDependencies} outdated, ${response.data.vulnerableCount} vulnerable).`);
        }
        
        // Update previous status for next comparison
        setPreviousVulnerabilityStatus(response.data.vulnerabilityCheckStatus);
      } catch (error) {
        console.error('Error fetching analysis:', error);
        setSnackbar({
          open: true,
          message: `Error loading analysis: ${error.message || 'Unknown error'}`,
          severity: 'error'
        });
        setVulnerabilitiesLoading(false);
      } finally {
        setLoading(false);
      }
    };

    console.log('Initial data fetch for analysisId:', analysisId);
    fetchAnalysisData();
  }, [analysisId, pollForVulnerabilityUpdates]);

  const handleTabChange = (event, newValue) => {
    console.log('Tab changed to:', newValue);
    setTabValue(newValue);
    
    // Auto refresh vulnerability status when switching to the vulnerabilities tab
    if (newValue === 3 && analysis && analysis.vulnerabilityCheckStatus === 'IN_PROGRESS') {
      console.log('Auto-refreshing vulnerability status when switching to vulnerabilities tab');
      refreshVulnerabilityStatus();
    }
  };

  // Filtering dependencies based on search query
  const filteredDependencies = dependencies.filter(dep => {
    const fullName = `${dep.groupId}:${dep.artifactId}:${dep.currentVersion}`.toLowerCase();
    return fullName.includes(searchQuery.toLowerCase());
  });
  
  // Collecting all vulnerabilities from dependencies for the vulnerabilities tab
  const allVulnerabilities = React.useMemo(() => {
    const vulnerabilities = [];
    dependencies.forEach(dep => {
      if (dep.vulnerabilities && dep.vulnerabilities.length > 0) {
        dep.vulnerabilities.forEach(vuln => {
          vulnerabilities.push({
            ...vuln,
            dependency: `${dep.groupId}:${dep.artifactId}:${dep.currentVersion}`
          });
        });
      }
    });
    return vulnerabilities;
  }, [dependencies]);

  // Add a refresh function
  const refreshVulnerabilityStatus = async () => {
    try {
      // Show loading state immediately for better UX
      setSnackbar({
        open: true,
        message: 'Refreshing vulnerability status...',
        severity: 'info'
      });
      
      setVulnerabilitiesLoading(true);
      
      // Add a small delay to ensure the UI reflects the loading state
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // Fetch the latest analysis data
      const response = await api.dependencyAnalysis.getById(analysisId);
      
      // Calculate the progress percentage for the UI
      const totalDeps = response.data.dependencies?.length || 0;
      const processedDeps = response.data.dependencies?.filter(d => d.vulnerableCount !== null).length || 0;
      const progressPercent = totalDeps > 0 ? Math.round((processedDeps / totalDeps) * 100) : 0;
      
      // Update the analysis with the latest data
      const totalVulnerabilities = response.data.dependencies 
        ? response.data.dependencies.reduce((total, dep) => total + (dep.vulnerableCount || 0), 0)
        : 0;
        
      setAnalysis({
        ...response.data,
        vulnerableCount: totalVulnerabilities,
        scanProgress: progressPercent,
        processedDependencies: processedDeps,
        totalDependencies: totalDeps,
        status: response.data.vulnerabilityCheckStatus === 'IN_PROGRESS' ? 'in_progress' : 
                response.data.vulnerabilityCheckStatus === 'COMPLETED' ? 'completed' :
                response.data.vulnerabilityCheckStatus === 'FAILED' ? 'failed' :
                response.data.status || 'completed'
      });
      
      if (response.data.dependencies) {
        setDependencies(response.data.dependencies);
      }
      
      // Also update the charts with the latest data
      fetchCharts();
      
      // Check if vulnerability processing is still in progress
      if (response.data.vulnerabilityCheckStatus === 'IN_PROGRESS') {
        // Start polling if still in progress
        pollForVulnerabilityUpdates();
        
        setSnackbar({
          open: true,
          message: `Vulnerability scan is ${progressPercent}% complete. Continuing to monitor progress.`,
          severity: 'info'
        });
      } else {
        setVulnerabilitiesLoading(false);
        
        setSnackbar({
          open: true,
          message: 'Vulnerability scan is complete!',
          severity: 'success'
        });
      }
    } catch (error) {
      console.error('Error refreshing vulnerability status:', error);
      setVulnerabilitiesLoading(false);
      setSnackbar({
        open: true,
        message: `Error refreshing data: ${error.message || 'Unknown error'}. Please try again.`,
        severity: 'error'
      });
    }
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  const handleGenerateReport = async () => {
    try {
      setReportGenerating(true);
      const response = await api.reports.generateFullReport(analysisId);
      
      if (response.data.reportPath) {
        // Download the report
        api.reports.downloadReport(response.data.reportPath);
        enqueueSnackbar('Report generated successfully', { variant: 'success' });
      } else {
        enqueueSnackbar('Failed to generate report', { variant: 'error' });
      }
    } catch (error) {
      console.error('Error generating report:', error);
      enqueueSnackbar(`Error generating report: ${error.message || 'Unknown error'}`, { variant: 'error' });
    } finally {
      setReportGenerating(false);
    }
  };

  // Fetch the restricted licenses from settings when the tab is rendered
  useEffect(() => {
    fetchRestrictedLicenses();
  }, []);
  
  const fetchRestrictedLicenses = async () => {
    try {
      const response = await api.settings.getSettings();
      console.log('Fetched settings for license checking:', response.data);
      if (response.data && response.data.restrictedLicenses) {
        setRestrictedLicenses(response.data.restrictedLicenses);
        console.log('Updated restricted licenses from settings:', response.data.restrictedLicenses);
      }
    } catch (error) {
      console.error('Error fetching restricted licenses from settings:', error);
    }
  };

  const handleDeleteAnalysis = async () => {
    if (!analysisId) {
      console.error('Cannot delete: Analysis ID is missing');
      enqueueSnackbar('Error: Cannot delete analysis because ID is missing', { variant: 'error' });
      return;
    }
    
    // Ensure we're using the correct numeric ID format for the API
    const numericId = parseInt(analysisId, 10);
    if (isNaN(numericId)) {
      console.error(`Invalid analysis ID format: ${analysisId}`);
      enqueueSnackbar('Error: Invalid analysis ID format', { variant: 'error' });
      return;
    }
    
    console.log(`Attempting to delete analysis with ID: ${numericId}`);
    
    try {
      // Show deletion in progress
      enqueueSnackbar('Deleting analysis...', { variant: 'info' });
      
      // Make the API call with explicit error handling and using the numeric ID
      const response = await api.dependencyAnalysis.delete(numericId);
      console.log('Delete API response:', response);
      
      // Success message and navigation
      enqueueSnackbar('Analysis deleted successfully', { variant: 'success' });
      
      // Wait a moment before navigating to ensure the user sees the success message
      setTimeout(() => {
        navigate('/history', { replace: true });
      }, 1000);
    } catch (error) {
      // Detailed error logging
      console.error('Error deleting analysis:', error);
      console.error('Error details:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data
      });
      
      // User-friendly error message
      const errorMsg = error.response?.data?.message || error.message || 'Unknown error';
      enqueueSnackbar(`Error deleting analysis: ${errorMsg}. Please try again or contact support.`, { 
        variant: 'error',
        autoHideDuration: 6000
      });
      
      // Close the dialog but don't navigate away
      setConfirmDelete(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton 
          onClick={() => navigate(-1)} 
          sx={{ mr: 1 }}
        >
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" component="h1" sx={{ flexGrow: 1 }}>
          Analysis Details
        </Typography>
        <Button 
          variant="outlined" 
          startIcon={<DownloadIcon />}
          sx={{ mr: 1 }}
          onClick={() => api.dependencyAnalysis.downloadUpdatedPom(analysisId)}
        >
          Export POM
        </Button>
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={() => {
            console.log('Delete button clicked, opening confirmation dialog');
            console.log('Analysis ID:', analysisId);
            setConfirmDelete(true);
          }}
        >
          Delete
        </Button>
      </Box>
      
      <Paper sx={{ p: 3, mb: 4 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={8}>
            <Typography variant="h6">
              Project: {analysis.projectName}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Analysis completed on {analysis.date}
            </Typography>
          </Grid>
          <Grid item xs={12} md={4} sx={{ display: 'flex', justifyContent: { xs: 'flex-start', md: 'flex-end' } }}>
            <Box>
              <Chip 
                label={
                  analysis.vulnerabilityCheckStatus === 'IN_PROGRESS' ? 'In Progress' :
                  analysis.status === 'completed' || analysis.status === 'COMPLETED' ? 'Completed' : 
                  analysis.status === 'failed' || analysis.status === 'FAILED' ? 'Failed' :
                  analysis.status === 'in_progress' || analysis.status === 'IN_PROGRESS' ? 'In Progress' :
                  'In Progress'
                } 
                color={
                  analysis.vulnerabilityCheckStatus === 'IN_PROGRESS' ? 'primary' :
                  analysis.status === 'completed' || analysis.status === 'COMPLETED' ? 'success' : 
                  analysis.status === 'failed' || analysis.status === 'FAILED' ? 'error' :
                  analysis.status === 'in_progress' || analysis.status === 'IN_PROGRESS' ? 'primary' :
                  'primary'
                }
                sx={{ mb: 1 }}
              />
              <Typography variant="body2" color="text.secondary">
                ID: {analysis.id}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>
      
      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={6} sm={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(33, 150, 243, 0.1)',
              borderRadius: 2,
            }}
          >
            <Typography color="text.secondary" variant="body2">
              Total Dependencies
            </Typography>
            <Typography variant="h5" sx={{ mt: 1 }}>
              {analysis.totalDependencies}
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={6} sm={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(255, 152, 0, 0.1)',
              borderRadius: 2,
            }}
          >
            <Typography color="text.secondary" variant="body2">
              Outdated
            </Typography>
            <Typography variant="h5" sx={{ mt: 1, color: 'warning.main' }}>
              {analysis.outdatedDependencies}
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={6} sm={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(239, 83, 80, 0.1)',
              borderRadius: 2,
            }}
          >
            <Typography color="text.secondary" variant="body2">
              Vulnerabilities
            </Typography>
            <Typography variant="h5" sx={{ mt: 1, color: 'error.main' }}>
              {analysis.vulnerableCount || 0}
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={6} sm={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(156, 39, 176, 0.1)',
              borderRadius: 2,
            }}
          >
            <Typography color="text.secondary" variant="body2">
              License Issues
            </Typography>
            <Typography variant="h5" sx={{ mt: 1, color: '#9c27b0' }}>
              {analysis.licenseIssues || 0}
            </Typography>
          </Paper>
        </Grid>
      </Grid>
      
      <Paper sx={{ mb: 4 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs 
            value={tabValue} 
            onChange={handleTabChange} 
            aria-label="analysis tabs"
            variant="scrollable"
            scrollButtons="auto"
          >
            <Tab label="Overview" />
            <Tab label="Dependencies" />
            <Tab label="Charts" />
            <Tab label="Vulnerabilities" />
            <Tab label="Licenses" />
          </Tabs>
        </Box>
        
        <TabPanel value={tabValue} index={0}>
          <Typography variant="h6" gutterBottom>
            Analysis Overview
          </Typography>
          
          {/* First Row - Dependency and Vulnerability Status Charts */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Dependency Status
                  </Typography>
                  {chartsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3, height: 250 }}>
                      <CircularProgress />
                    </Box>
                  ) : charts.dependencyUpdates ? (
                    <Box sx={{ height: 300 }}>
                      <DependencyStatusChart 
                        data={charts.dependencyUpdates} 
                        loading={chartsLoading} 
                        height={300}
                      />
                    </Box>
                  ) : (
                    <Box
                      sx={{
                        height: 250,
                        bgcolor: 'rgba(33, 150, 243, 0.1)',
                        borderRadius: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        p: 2,
                      }}
                    >
                      <Typography color="text.secondary">
                        No chart available
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Vulnerability Distribution
                  </Typography>
                  {chartsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3, height: 250 }}>
                      <CircularProgress />
                    </Box>
                  ) : analysis && analysis.dependencies && analysis.dependencies.some(dep => dep.vulnerableCount > 0) ? (
                    <Box sx={{ height: 300 }}>
                      <VulnerabilityByDependencyChart 
                        data={analysis} 
                        loading={chartsLoading} 
                        height={300}
                      />
                    </Box>
                  ) : (
                    <Box
                      sx={{
                        height: 250,
                        bgcolor: 'rgba(244, 67, 54, 0.1)',
                        borderRadius: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        p: 2,
                      }}
                    >
                      <Typography color="text.secondary">
                        No vulnerabilities found in dependencies
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Second Row - Vulnerability Severity and License Distribution Charts */}
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Vulnerability Severity
                  </Typography>
                  {chartsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3, height: 250 }}>
                      <CircularProgress />
                    </Box>
                  ) : charts.vulnerabilitySeverity && (
                      Array.isArray(charts.vulnerabilitySeverity.data) && 
                      charts.vulnerabilitySeverity.data.length > 0
                    ) ? (
                    <Box sx={{ height: 300 }}>
                      <VulnerabilitySeverityChart 
                        data={charts.vulnerabilitySeverity} 
                        loading={chartsLoading} 
                        height={300}
                      />
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1, textAlign: 'center' }}>
                        {charts.vulnerabilitySeverity.cleanSummary 
                          ? `${charts.vulnerabilitySeverity.cleanSummary}` 
                          : (charts.vulnerabilitySeverity.summary && !charts.vulnerabilitySeverity.summary.includes('undefined'))
                            ? charts.vulnerabilitySeverity.summary
                            : "Vulnerabilities by severity level"}
                      </Typography>
                    </Box>
                  ) : analysis && analysis.dependencies && analysis.dependencies.some(dep => 
                      dep.vulnerabilities && dep.vulnerabilities.length > 0) ? (
                    <Box sx={{ height: 300 }}>
                      <VulnerabilitySeverityChart 
                        data={analysis} 
                        loading={chartsLoading} 
                        height={300}
                      />
                    </Box>
                  ) : (
                    <Box
                      sx={{
                        height: 250,
                        bgcolor: 'rgba(244, 67, 54, 0.1)',
                        borderRadius: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        p: 2,
                      }}
                    >
                      <Typography color="text.secondary">
                        No vulnerability severity data available
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    License Distribution
                  </Typography>
                  {chartsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3, height: 250 }}>
                      <CircularProgress />
                    </Box>
                  ) : charts.licenses ? (
                    <Box sx={{ height: 300 }}>
                      <LicenseDistributionChart 
                        data={charts.licenses} 
                        loading={chartsLoading} 
                        height={300}
                        showLegend={false}
                      />
                    </Box>
                  ) : (
                    <Box
                      sx={{
                        height: 250,
                        bgcolor: 'rgba(76, 175, 80, 0.1)',
                        borderRadius: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        p: 2,
                      }}
                    >
                      <Typography color="text.secondary">
                        No license data available
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
          
          <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>
            Summary
          </Typography>
          <Typography variant="body1" paragraph>
            This analysis found {analysis.outdatedDependencies} outdated dependencies, {analysis.vulnerableCount || 0} security vulnerabilities, and {analysis.licenseIssues || 0} license compliance issues.
            {analysis.vulnerableCount > 0 && (
              <> The "Vulnerability Distribution" chart shows which dependencies contribute most to your security risks.</>
            )}
          </Typography>
          <Typography variant="body1" paragraph>
            It's recommended to update the dependencies to their latest versions to resolve security issues and take advantage of the latest features and bug fixes.
            {analysis.vulnerableCount > 0 && (
              <> Focus on updating dependencies with the highest number of vulnerabilities first for maximum impact.</>
            )}
          </Typography>
          
          <Box sx={{ mt: 2 }}>
            <Button 
              variant="contained" 
              onClick={handleGenerateReport}
              disabled={reportGenerating}
              startIcon={reportGenerating ? <CircularProgress size={20} /> : null}
            >
              {reportGenerating ? 'Generating Report...' : 'Generate Full Report'}
            </Button>
          </Box>
        </TabPanel>
        
        <TabPanel value={tabValue} index={1}>
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <TextField
              placeholder="Search dependencies..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="small"
              sx={{ width: '40%' }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
            <Button
              variant="contained"
              color="primary"
              startIcon={<DownloadIcon />}
              onClick={() => api.dependencyAnalysis.downloadUpdatedPom(analysisId)}
            >
              Download Updated POM
            </Button>
          </Box>
          
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Group ID</TableCell>
                  <TableCell>Artifact ID</TableCell>
                  <TableCell>Current Version</TableCell>
                  <TableCell>Latest Version</TableCell>
                  <TableCell>Scope</TableCell>
                  <TableCell>License</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredDependencies.map((dependency) => (
                  <TableRow key={dependency.id} hover>
                    <TableCell>{dependency.groupId}</TableCell>
                    <TableCell>{dependency.artifactId}</TableCell>
                    <TableCell>
                      {dependency.currentVersion && dependency.currentVersion.includes('MANAGED_BY_BOM') ? (
                        <BomManagedVersion 
                          version={dependency.currentVersion}
                          groupId={dependency.groupId} 
                          artifactId={dependency.artifactId}
                          estimatedVersion={dependency.estimatedVersion}
                        />
                      ) : (
                        dependency.currentVersion
                      )}
                    </TableCell>
                    <TableCell>
                      {dependency.latestVersion ? (
                        dependency.status === "BOM Managed" ? (
                          <Typography color="info.main" fontStyle="italic">
                            {dependency.latestVersion}
                          </Typography>
                        ) : dependency.isOutdated ? (
                          <Typography color="warning.main" fontWeight="medium">
                            {dependency.latestVersion}
                          </Typography>
                        ) : (
                          dependency.latestVersion
                        )
                      ) : (
                        <Typography color="text.secondary" fontStyle="italic">
                          Unknown
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={dependency.scope || 'compile'} 
                        size="small" 
                        color={dependency.scope === 'test' ? 'secondary' : 
                               dependency.scope === 'provided' ? 'info' : 'default'}
                        variant="outlined" 
                        sx={{ fontWeight: 'normal' }}
                      />
                    </TableCell>
                    <TableCell>{!dependency.license || dependency.license === 'null' ? 'Unknown' : dependency.license}</TableCell>
                    <TableCell>
                      <Chip 
                        label={dependency.status || (
                          dependency.isVulnerable ? 'Vulnerable' : 
                          dependency.isOutdated ? 'Outdated' :
                          dependency.latestVersion ? 'Up-to-date' : 'Unknown'
                        )} 
                        size="small"
                        color={
                          dependency.status === 'BOM Managed' ? 'info' :
                          dependency.status === 'Vulnerable' || dependency.isVulnerable ? 'error' : 
                          dependency.status === 'Outdated' || dependency.isOutdated ? 'warning' :
                          dependency.status === 'Up-to-date' ? 'success' : 'default'
                        }
                      />
                    </TableCell>
                  </TableRow>
                ))}
                {filteredDependencies.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      No dependencies found
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>
        
        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" gutterBottom>
            Visualization Charts
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Dependency Update Status
                  </Typography>
                  <Box sx={{ height: 400 }}>
                    <DependencyStatusChart 
                      data={charts.dependencyUpdates || analysis} 
                      loading={chartsLoading} 
                      height={400}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Vulnerability Distribution
                  </Typography>
                  <Box sx={{ height: 400 }}>
                    <VulnerabilityByDependencyChart 
                      data={analysis} 
                      loading={chartsLoading} 
                      height={400}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    Vulnerability Severity
                  </Typography>
                  {chartsLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3, height: 250 }}>
                      <CircularProgress />
                    </Box>
                  ) : charts.vulnerabilitySeverity && (
                      Array.isArray(charts.vulnerabilitySeverity.data) && 
                      charts.vulnerabilitySeverity.data.length > 0
                    ) ? (
                    <Box sx={{ height: 300 }}>
                      <VulnerabilitySeverityChart 
                        data={charts.vulnerabilitySeverity} 
                        loading={chartsLoading} 
                        height={300}
                      />
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1, textAlign: 'center' }}>
                        {charts.vulnerabilitySeverity.cleanSummary 
                          ? `${charts.vulnerabilitySeverity.cleanSummary}` 
                          : (charts.vulnerabilitySeverity.summary && !charts.vulnerabilitySeverity.summary.includes('undefined'))
                            ? charts.vulnerabilitySeverity.summary
                            : "Vulnerabilities by severity level"}
                      </Typography>
                    </Box>
                  ) : analysis && analysis.dependencies && analysis.dependencies.some(dep => 
                      dep.vulnerabilities && dep.vulnerabilities.length > 0) ? (
                    <Box sx={{ height: 300 }}>
                      <VulnerabilitySeverityChart 
                        data={analysis} 
                        loading={chartsLoading} 
                        height={300}
                      />
                    </Box>
                  ) : (
                    <Box
                      sx={{
                        height: 250,
                        bgcolor: 'rgba(244, 67, 54, 0.1)',
                        borderRadius: 1,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexDirection: 'column',
                        p: 2,
                      }}
                    >
                      <Typography color="text.secondary">
                        No vulnerability severity data available
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" gutterBottom>
                    License Distribution
                  </Typography>
                  <Box sx={{ height: 400 }}>
                    <LicenseDistributionChart 
                      data={charts.licenses || analysis} 
                      loading={chartsLoading} 
                      height={400}
                      showLegend={false}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
        
        <TabPanel value={tabValue} index={3}>
          {vulnerabilitiesLoading ? (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 6 }}>
              <CircularProgress size={40} sx={{ mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                Scanning for vulnerabilities...
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 3 }}>
                Security scanning is running in the background. You can navigate away from this page and come back later to see the results.
                Basic dependency information is already available in the Dependencies tab.
              </Typography>
              
              {analysis && analysis.vulnerabilityCheckStatus === 'IN_PROGRESS' && (
                <Box sx={{ mt: 3, width: '100%', maxWidth: 500, mx: 'auto' }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2" color="text.secondary">Scanning dependencies...</Typography>
                    <Typography variant="body2" color="primary">
                      {analysis.processedDependencies || dependencies.filter(d => d.vulnerableCount !== null).length} / {analysis.totalDependencies || dependencies.length} checked
                    </Typography>
                  </Box>
                  
                  <Box sx={{ width: '100%', bgcolor: 'background.paper', borderRadius: 1, p: 1 }}>
                    <Box sx={{ 
                      height: 8, 
                      width: `${analysis.scanProgress || (dependencies.filter(d => d.vulnerableCount !== null).length / dependencies.length) * 100}%`, 
                      bgcolor: 'primary.main',
                      borderRadius: 5,
                      transition: 'width 0.5s ease-in-out'
                    }} />
                  </Box>
                  
                  <Typography variant="caption" align="center" sx={{ display: 'block', mt: 1 }}>
                    {analysis.scanProgress ? `${analysis.scanProgress}% complete` : 'Initializing scan...'}
                  </Typography>
                  
                  <Box sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}>
                    <Button 
                      variant="outlined" 
                      onClick={refreshVulnerabilityStatus}
                    >
                      Refresh Status
                    </Button>
                  </Box>
                  
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 2, textAlign: 'center', fontStyle: 'italic' }}>
                    Tip: This process runs in the background and the page will update automatically when completed.
                  </Typography>
                </Box>
              )}
            </Box>
          ) : allVulnerabilities.length > 0 ? (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Dependency</TableCell>
                    <TableCell>Vulnerability</TableCell>
                    <TableCell>Severity</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Affected Versions</TableCell>
                    <TableCell>Fixed In</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {allVulnerabilities.map((vuln, index) => (
                    <TableRow key={`${vuln.id || index}`} hover>
                      <TableCell>{vuln.dependency}</TableCell>
                      <TableCell>{vuln.name}</TableCell>
                      <TableCell>
                        <Chip 
                          label={vuln.severity} 
                          color={
                            vuln.severity === 'CRITICAL' ? 'error' :
                            vuln.severity === 'HIGH' ? 'error' :
                            vuln.severity === 'MEDIUM' ? 'warning' :
                            'info'
                          } 
                          size="small" 
                        />
                      </TableCell>
                      <TableCell>{vuln.description}</TableCell>
                      <TableCell>{vuln.affectedVersions}</TableCell>
                      <TableCell>{vuln.fixedInVersion}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Box sx={{ p: 4, textAlign: 'center' }}>
              <Typography variant="h6" color="text.secondary">
                No vulnerabilities found
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                All dependencies appear to be secure. Continue to monitor for new vulnerabilities.
              </Typography>
            </Box>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">
              Restricted License Issues
            </Typography>
          </Box>
          
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Dependency</TableCell>
                  <TableCell>License</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Recommendation</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {console.log('Current restricted licenses for filtering:', restrictedLicenses)}
                {analysis.dependencies && analysis.dependencies.filter(dep => {
                  // Skip dependencies without licenses
                  if (!dep.license) {
                    console.log(`Dependency ${dep.groupId}:${dep.artifactId} has no license`);
                    return true; // Missing licenses should be flagged
                  }
                  
                  // Create a normalized version of license for easier comparison
                  const normalizedLicense = dep.license.toUpperCase();
                  
                  // Check if the license contains any of the restricted licenses
                  const isRestricted = restrictedLicenses.some(restricted => 
                    normalizedLicense.includes(restricted.toUpperCase())
                  );
                  
                  if (isRestricted) {
                    console.log(`Dependency ${dep.groupId}:${dep.artifactId} has restricted license: ${dep.license}`);
                  }
                  return isRestricted;
                }).map(dep => (
                  <TableRow key={dep.id} hover>
                    <TableCell>
                      {dep.groupId}:{dep.artifactId}:{dep.version || dep.currentVersion}
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={dep.license || 'Unknown'} 
                        size="small"
                        color={!dep.license ? 'warning' : 
                               restrictedLicenses.some(restricted => 
                                 (dep.license.toUpperCase().includes(restricted.toUpperCase())) && 
                                 (restricted.toUpperCase() === 'GPL' || restricted.toUpperCase() === 'AGPL')
                               ) ? 'error' : 
                               restrictedLicenses.some(restricted => 
                                 dep.license.toUpperCase().includes(restricted.toUpperCase())
                               ) ? 'warning' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      {!dep.license ? 'Missing License' : 
                       restrictedLicenses.some(restricted => 
                         dep.license.toUpperCase().includes(restricted.toUpperCase())
                       ) ? 'Restricted License' : 'Compatible License'}
                    </TableCell>
                    <TableCell>
                      {!dep.license ? 'Verify license information with the library author' : 
                       dep.license.toUpperCase().includes('GPL') || dep.license.toUpperCase().includes('AGPL') ? 
                         'Consider using a commercial license or alternative library' :
                       dep.license.toUpperCase().includes('LGPL') ? 
                         'Review LGPL compliance requirements for your distribution' :
                       'Review license terms and consult legal counsel if necessary'}
                    </TableCell>
                  </TableRow>
                ))}
                {(!analysis.dependencies || analysis.dependencies.length === 0 || 
                  analysis.dependencies.filter(dep => {
                    if (!dep.license) return true;
                    const normalizedLicense = dep.license.toUpperCase();
                    return restrictedLicenses.some(restricted => 
                      normalizedLicense.includes(restricted.toUpperCase())
                    );
                  }).length === 0) && (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      No license issues found
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>
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
      
      {/* Delete confirmation dialog */}
      <Dialog
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">Delete Analysis</DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete this analysis? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(false)}>Cancel</Button>
          <Button onClick={handleDeleteAnalysis} color="error" autoFocus>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default AnalysisDetail; 