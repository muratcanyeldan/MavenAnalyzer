import React, { useState, useEffect } from 'react';
import {
  Typography,
  Paper,
  Box,
  Grid,
  Card,
  CardContent,
  CardHeader,
  CircularProgress,
  Button,
  Tabs,
  Tab,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

// Tab Panel component for the tabs
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`chart-tabpanel-${index}`}
      aria-labelledby={`chart-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ pt: 3 }}>{children}</Box>
      )}
    </div>
  );
}

const Charts = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [analyses, setAnalyses] = useState([]);
  // eslint-disable-next-line no-unused-vars
  const [projects, setProjects] = useState([]);
  const [selectedAnalysisId, setSelectedAnalysisId] = useState('');
  const [tabValue, setTabValue] = useState(0);
  const [charts, setCharts] = useState({
    dependencyUpdates: null,
    vulnerabilities: null,
    licenses: null
  });

  // Fetch analyses and projects
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch all projects
        const projectsResponse = await api.projects.getAll();
        setProjects(projectsResponse.data);

        // Fetch all analyses
        const analysesResponse = await api.dependencyAnalysis.getAll();
        const analysesData = analysesResponse.data;
        
        // Enrich analyses with project names
        const enrichedAnalyses = analysesData.map(analysis => {
          const project = projectsResponse.data.find(p => p.id === analysis.projectId);
          return {
            ...analysis,
            projectName: project ? project.name : `Project ${analysis.projectId}`,
            id: analysis.analysisId, // Map analysisId to id for consistency
            date: new Date(analysis.analysisDate).toLocaleString() // Format date for display
          };
        });
        
        setAnalyses(enrichedAnalyses);
        
        // Set default selected analysis to the most recent one if available
        if (enrichedAnalyses.length > 0) {
          setSelectedAnalysisId(enrichedAnalyses[0].id);
        }
      } catch (error) {
        console.error('Error fetching data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Fetch charts when selected analysis changes
  useEffect(() => {
    const fetchCharts = async () => {
      if (!selectedAnalysisId) return;
      
      setLoading(true);
      try {
        // Fetch different chart types
        const dependencyUpdatesResponse = await api.charts.getDependencyUpdates(selectedAnalysisId);
        const vulnerabilitiesResponse = await api.charts.getVulnerabilities(selectedAnalysisId);
        const licenseDistributionResponse = await api.charts.getLicenseDistribution(selectedAnalysisId);
        
        setCharts({
          dependencyUpdates: dependencyUpdatesResponse.data,
          vulnerabilities: vulnerabilitiesResponse.data,
          licenses: licenseDistributionResponse.data
        });
      } catch (error) {
        console.error('Error fetching charts:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchCharts();
  }, [selectedAnalysisId]);

  const handleChangeTab = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleAnalysisChange = (event) => {
    setSelectedAnalysisId(event.target.value);
  };

  const getSelectedAnalysis = () => {
    return analyses.find(analysis => analysis.id === selectedAnalysisId) || {};
  };

  return (
    <>
      <Typography variant="h4" component="h1" gutterBottom>
        Visualization Charts
      </Typography>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Select Analysis
        </Typography>
        
        {loading && analyses.length === 0 ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <FormControl fullWidth>
            <InputLabel>Analysis</InputLabel>
            <Select
              value={selectedAnalysisId}
              onChange={handleAnalysisChange}
              label="Analysis"
            >
              {analyses.map(analysis => (
                <MenuItem key={analysis.id} value={analysis.id}>
                  {analysis.projectName} - {analysis.date}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}
      </Paper>
      
      {selectedAnalysisId && (
        <>
          <Paper sx={{ p: 0, mb: 3 }}>
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
              <Tabs value={tabValue} onChange={handleChangeTab} aria-label="chart tabs">
                <Tab label="Dependency Updates" />
                <Tab label="Vulnerabilities" />
                <Tab label="License Distribution" />
              </Tabs>
            </Box>
            
            <Box sx={{ p: 3 }}>
              <TabPanel value={tabValue} index={0}>
                <Typography variant="h6" gutterBottom>
                  Dependency Update Status
                </Typography>
                
                {loading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                    <CircularProgress />
                  </Box>
                ) : charts.dependencyUpdates ? (
                  <Box sx={{ mt: 2 }}>
                    <img 
                      src={`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api'}/static/charts/${charts.dependencyUpdates.chartPath}`} 
                      alt="Dependency Updates Chart" 
                      style={{ maxWidth: '100%', height: 'auto' }}
                    />
                    <Typography variant="body1" sx={{ mt: 2 }}>
                      {charts.dependencyUpdates.description}
                    </Typography>
                  </Box>
                ) : (
                  <Typography>
                    No dependency update chart available for this analysis.
                  </Typography>
                )}
                
                <Box sx={{ mt: 3 }}>
                  <Grid container spacing={3}>
                    <Grid item xs={12} md={4}>
                      <Card>
                        <CardHeader title="Up-to-date" />
                        <CardContent>
                          <Typography variant="h3" color="success.main" align="center">
                            {getSelectedAnalysis().upToDateDependencies || 0}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card>
                        <CardHeader title="Outdated" />
                        <CardContent>
                          <Typography variant="h3" color="warning.main" align="center">
                            {getSelectedAnalysis().outdatedDependencies || 0}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <Card>
                        <CardHeader title="Unknown" />
                        <CardContent>
                          <Typography variant="h3" color="text.secondary" align="center">
                            {getSelectedAnalysis().unidentifiedDependencies || 0}
                          </Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                </Box>
              </TabPanel>
              
              <TabPanel value={tabValue} index={1}>
                <Typography variant="h6" gutterBottom>
                  Vulnerability Status
                </Typography>
                
                {loading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                    <CircularProgress />
                  </Box>
                ) : charts.vulnerabilities ? (
                  <Box sx={{ mt: 2 }}>
                    <img 
                      src={`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api'}/static/charts/${charts.vulnerabilities.chartPath}`} 
                      alt="Vulnerabilities Chart" 
                      style={{ maxWidth: '100%', height: 'auto' }}
                    />
                    <Typography variant="body1" sx={{ mt: 2 }}>
                      {charts.vulnerabilities.description}
                    </Typography>
                  </Box>
                ) : (
                  <Typography>
                    No vulnerability chart available for this analysis.
                  </Typography>
                )}
                
                <Box sx={{ mt: 3 }}>
                  <Card>
                    <CardHeader title="Vulnerability Summary" />
                    <CardContent>
                      <Typography variant="body1">
                        Total vulnerabilities found: <strong>{getSelectedAnalysis().vulnerableCount || 0}</strong>
                      </Typography>
                    </CardContent>
                  </Card>
                </Box>
              </TabPanel>
              
              <TabPanel value={tabValue} index={2}>
                <Typography variant="h6" gutterBottom>
                  License Distribution
                </Typography>
                
                {loading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                    <CircularProgress />
                  </Box>
                ) : charts.licenses ? (
                  <Box sx={{ mt: 2 }}>
                    <img 
                      src={`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api'}/static/charts/${charts.licenses.chartPath}`} 
                      alt="License Distribution Chart" 
                      style={{ maxWidth: '100%', height: 'auto' }}
                    />
                    <Typography variant="body1" sx={{ mt: 2 }}>
                      {charts.licenses.description}
                    </Typography>
                  </Box>
                ) : (
                  <Typography>
                    No license distribution chart available for this analysis.
                  </Typography>
                )}
              </TabPanel>
            </Box>
          </Paper>
          
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
            <Button 
              variant="contained" 
              onClick={() => navigate(`/analysis/${selectedAnalysisId}`)}
            >
              View Analysis Details
            </Button>
          </Box>
        </>
      )}
    </>
  );
};

export default Charts; 