import React, { useState, useEffect } from 'react';
import { 
  Typography, 
  Grid, 
  Paper, 
  Card, 
  CardContent, 
  CardHeader,
  Button,
  Box,
  CircularProgress,
  Divider
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AssessmentIcon from '@mui/icons-material/Assessment';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import WarningIcon from '@mui/icons-material/Warning';
import UpdateIcon from '@mui/icons-material/Update';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import api from '../services/api';
import { ResponsivePie } from '@nivo/pie';
import { format } from 'date-fns';

const DependencyStatusChart = ({ data }) => {
  // Skip rendering if we don't have valid data
  if (!data || !data.data || !Array.isArray(data.data) || data.data.length === 0) {
    return (
      <Box
        sx={{
          height: 200,
          bgcolor: 'rgba(33, 150, 243, 0.1)',
          borderRadius: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Typography color="text.secondary">
          No data available for chart visualization
        </Typography>
      </Box>
    );
  }

  // Use the data directly from the PieChartDataResponse format
  const chartData = data.data;

  return (
    <Box sx={{ height: 200 }}>
      <ResponsivePie
        data={chartData}
        margin={{ top: 20, right: 120, bottom: 20, left: 40 }}
        innerRadius={0.5}
        padAngle={0.7}
        cornerRadius={3}
        activeOuterRadiusOffset={8}
        colors={{ datum: 'data.color' }}
        borderWidth={1}
        borderColor={{ from: 'color', modifiers: [['darker', 0.2]] }}
        arcLinkLabelsSkipAngle={10}
        arcLinkLabelsTextColor="#333333"
        arcLinkLabelsThickness={2}
        arcLinkLabelsColor={{ from: 'color' }}
        arcLabelsSkipAngle={10}
        arcLabelsTextColor={{ from: 'color', modifiers: [['darker', 2]] }}
        enableArcLabels={false}
        legends={[
          {
            anchor: 'right',
            direction: 'column',
            justify: false,
            translateX: 30,
            translateY: 0,
            itemsSpacing: 5,
            itemWidth: 80,
            itemHeight: 20,
            itemTextColor: '#666',
            itemDirection: 'left-to-right',
            itemOpacity: 1,
            symbolSize: 15,
            symbolShape: 'circle'
          }
        ]}
      />
    </Box>
  );
};

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProjects: 0,
    recentAnalyses: [],
    totalDependencies: 0,
    outdatedDependencies: 0,
    vulnerabilities: 0,
  });
  const [chartData, setChartData] = useState(null);
  const [chartLoading, setChartLoading] = useState(false);

  useEffect(() => {
    // Fetch real data from the API
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const response = await api.dashboard.getStats();
        setStats({
          totalProjects: response.data.totalProjects,
          recentAnalyses: response.data.recentAnalyses,
          totalDependencies: response.data.totalDependencies,
          outdatedDependencies: response.data.outdatedDependencies,
          vulnerabilities: response.data.vulnerabilities,
        });
        
        // If we have recent analyses, use the first one to get chart data
        if (response.data.recentAnalyses && response.data.recentAnalyses.length > 0) {
          const latestAnalysis = response.data.recentAnalyses[0];
          fetchChartData(latestAnalysis.analysisId);
        }
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
        setLoading(false);
      }
    };
    
    fetchDashboardData();
  }, []);
  
  const fetchChartData = async (analysisId) => {
    try {
      setChartLoading(true);
      // Get the dependency status data for the chart
      const response = await api.charts.getDependencyStatusData(analysisId);
      setChartData(response.data);
      setChartLoading(false);
    } catch (error) {
      console.error('Error fetching chart data:', error);
      setChartLoading(false);
    }
  };

  // Format the date to a more readable format
  const formatDate = (dateString) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch (e) {
      return dateString;
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
      <Typography variant="h4" component="h1" gutterBottom>
        Dashboard
      </Typography>
      
      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
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
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: 'primary.main',
                  color: 'white',
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  mr: 2,
                }}
              >
                <FolderOpenIcon />
              </Box>
              <Typography variant="h5">{stats.totalProjects}</Typography>
            </Box>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Projects
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(76, 175, 80, 0.1)',
              borderRadius: 2,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: '#4caf50',
                  color: 'white',
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  mr: 2,
                }}
              >
                <AssessmentIcon />
              </Box>
              <Typography variant="h5">{stats.totalDependencies}</Typography>
            </Box>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Total Dependencies
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
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
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: '#ff9800',
                  color: 'white',
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  mr: 2,
                }}
              >
                <UpdateIcon />
              </Box>
              <Typography variant="h5">{stats.outdatedDependencies}</Typography>
            </Box>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Outdated Dependencies
            </Typography>
          </Paper>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Paper
            elevation={0}
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              bgcolor: 'rgba(244, 67, 54, 0.1)',
              borderRadius: 2,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  bgcolor: '#f44336',
                  color: 'white',
                  width: 40,
                  height: 40,
                  borderRadius: '50%',
                  mr: 2,
                }}
              >
                <WarningIcon />
              </Box>
              <Typography variant="h5">{stats.vulnerabilities}</Typography>
            </Box>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              Vulnerabilities
            </Typography>
          </Paper>
        </Grid>
      </Grid>
      
      <Grid container spacing={3}>
        {/* Recent Analyses */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardHeader 
              title="Recent Analyses" 
              action={
                <Button 
                  component={RouterLink} 
                  to="/analysis-history" 
                  size="small" 
                  endIcon={<ArrowForwardIcon />}
                >
                  View All
                </Button>
              }
            />
            <Divider />
            <CardContent>
              {stats.recentAnalyses && stats.recentAnalyses.length > 0 ? (
                stats.recentAnalyses.map((analysis) => (
                  <Box key={analysis.analysisId} sx={{ mb: 2 }}>
                    <Grid container spacing={2}>
                      <Grid item xs={8}>
                        <Typography variant="subtitle1" component={RouterLink} to={`/analysis/${analysis.analysisId}`} sx={{ textDecoration: 'none', color: 'primary.main' }}>
                          {analysis.projectName}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Analyzed on {formatDate(analysis.analysisDate)}
                        </Typography>
                      </Grid>
                      <Grid item xs={4} sx={{ textAlign: 'right' }}>
                        <Typography variant="body2">
                          {analysis.totalDependencies} dependencies
                        </Typography>
                        <Typography variant="body2" color="warning.main">
                          {analysis.outdatedDependencies} outdated
                        </Typography>
                      </Grid>
                    </Grid>
                    <Divider sx={{ mt: 2 }} />
                  </Box>
                ))
              ) : (
                <Typography>No recent analyses found</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
        
        {/* Charts */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardHeader 
              title="Dependency Overview" 
              action={
                <Button 
                  component={RouterLink} 
                  to="/charts" 
                  size="small" 
                  endIcon={<ArrowForwardIcon />}
                >
                  View Charts
                </Button>
              }
            />
            <Divider />
            <CardContent>
              {chartLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                  <CircularProgress />
                </Box>
              ) : chartData ? (
                <DependencyStatusChart data={chartData} />
              ) : (
                <Box
                  sx={{
                    height: 200,
                    bgcolor: 'rgba(33, 150, 243, 0.1)',
                    borderRadius: 1,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Typography color="text.secondary">
                    No chart data available
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {/* Quick Actions */}
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6" gutterBottom>
          Quick Actions
        </Typography>
        <Grid container spacing={2}>
          <Grid item>
            <Button 
              variant="contained" 
              component={RouterLink} 
              to="/projects"
              state={{ openNewProjectDialog: true }}
            >
              Create New Project
            </Button>
          </Grid>
          <Grid item>
            <Button 
              variant="outlined" 
              component={RouterLink} 
              to="/projects"
            >
              View All Projects
            </Button>
          </Grid>
          <Grid item>
            <Button 
              variant="outlined" 
              component={RouterLink} 
              to="/analysis-history"
            >
              View Analysis History
            </Button>
          </Grid>
        </Grid>
      </Box>
    </>
  );
};

export default Dashboard; 