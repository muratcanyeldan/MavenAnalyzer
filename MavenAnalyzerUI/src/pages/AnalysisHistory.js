import React, { useState, useEffect } from 'react';
import {
  Typography,
  Paper,
  Box,
  Grid,
  Card,
  CardContent,
  CircularProgress,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  InputAdornment,
  Select,
  FormControl,
  InputLabel,
  MenuItem,
  IconButton,
  Pagination,
  Tooltip,
  Snackbar,
  Alert,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import SearchIcon from '@mui/icons-material/Search';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DownloadIcon from '@mui/icons-material/Download';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../services/api';

const AnalysisHistory = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [analyses, setAnalyses] = useState([]);
  const [projects, setProjects] = useState([]);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({
    project: 'all',
    date: 'all',
  });
  const [page, setPage] = useState(1);
  const [rowsPerPage] = useState(10);
  const [reportGenerating, setReportGenerating] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [analysisToDelete, setAnalysisToDelete] = useState(null);

  useEffect(() => {
    // Fetch analyses and projects from the API
    const fetchData = async () => {
      setLoading(true);
      try {
        // First fetch projects to get project names for displaying in the table
        const projectsResponse = await api.projects.getAll();
        const projectsData = projectsResponse.data;
        setProjects(projectsData);

        // Then fetch all analyses
        const analysesResponse = await api.dependencyAnalysis.getAll();
        const analysesData = analysesResponse.data;
        
        // Enrich analyses with project names
        const enrichedAnalyses = analysesData.map(analysis => {
          const project = projectsData.find(p => p.id === analysis.projectId);
          return {
            ...analysis,
            projectName: project ? project.name : `Project ${analysis.projectId}`,
            id: analysis.analysisId, // Map analysisId to id for consistency
            date: new Date(analysis.analysisDate).toLocaleString(), // Format date for display
            status: analysis.totalDependencies > 0 ? 'completed' : 'in-progress'
          };
        });
        
        setAnalyses(enrichedAnalyses);
      } catch (error) {
        console.error('Error fetching data:', error);
        setAnalyses([]);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, []);

  const handlePageChange = (event, newPage) => {
    setPage(newPage);
  };

  const handleFilterChange = (event) => {
    setFilters({
      ...filters,
      [event.target.name]: event.target.value,
    });
  };

  // Filter analyses based on search and filters
  const filteredAnalyses = analyses.filter((analysis) => {
    const matchesSearch = 
      (analysis.projectName && analysis.projectName.toLowerCase().includes(search.toLowerCase())) ||
      (analysis.id && analysis.id.toString().includes(search));
      
    const matchesProject = 
      filters.project === 'all' || 
      (analysis.projectId && analysis.projectId.toString() === filters.project);
      
    return matchesSearch && matchesProject;
  });

  // Get unique project IDs for filter dropdown
  const projectOptions = [
    { id: 'all', name: 'All Projects' },
    ...projects.map(project => ({
      id: project.id.toString(),
      name: project.name
    }))
  ];

  // Paginate results
  const paginatedAnalyses = filteredAnalyses.slice(
    (page - 1) * rowsPerPage,
    page * rowsPerPage
  );

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  const handleDownloadReport = async (analysisId) => {
    try {
      // Set report generating state for this specific analysis
      setReportGenerating(prev => ({ ...prev, [analysisId]: true }));
      
      // Call API to generate report
      const response = await api.reports.generateFullReport(analysisId);
      
      if (response.data.reportPath) {
        // Download the report
        api.reports.downloadReport(response.data.reportPath);
        setSnackbar({
          open: true,
          message: 'Report generated and downloaded successfully',
          severity: 'success'
        });
      } else {
        setSnackbar({
          open: true,
          message: 'Failed to generate report',
          severity: 'error'
        });
      }
    } catch (error) {
      console.error('Error generating report:', error);
      setSnackbar({
        open: true,
        message: `Error generating report: ${error.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      // Clear report generating state for this specific analysis
      setReportGenerating(prev => ({ ...prev, [analysisId]: false }));
    }
  };

  const handleDeleteAnalysis = async () => {
    if (!analysisToDelete) {
      console.error('No analysis selected for deletion');
      return;
    }
    
    // Ensure we're using the correct numeric ID format for the API
    const numericId = parseInt(analysisToDelete, 10);
    if (isNaN(numericId)) {
      console.error(`Invalid analysis ID format: ${analysisToDelete}`);
      setSnackbar({
        open: true,
        message: 'Error: Invalid analysis ID format',
        severity: 'error'
      });
      return;
    }
    
    console.log(`Attempting to delete analysis with ID: ${numericId}`);
    
    try {
      // Show deletion in progress
      setSnackbar({
        open: true,
        message: 'Deleting analysis...',
        severity: 'info'
      });
      
      // Make the API call
      const response = await api.dependencyAnalysis.delete(numericId);
      console.log('Delete API response:', response);
      
      // Success message
      setSnackbar({
        open: true,
        message: 'Analysis deleted successfully',
        severity: 'success'
      });
      
      // Update the analyses list by removing the deleted one
      const updatedAnalyses = analyses.filter(analysis => analysis.id !== numericId);
      setAnalyses(updatedAnalyses);
      
      // Calculate pagination after deletion
      const filteredAnalyses = updatedAnalyses.filter((analysis) => {
        const matchesSearch = 
          (analysis.projectName && analysis.projectName.toLowerCase().includes(search.toLowerCase())) ||
          (analysis.id && analysis.id.toString().includes(search));
          
        const matchesProject = 
          filters.project === 'all' || 
          (analysis.projectId && analysis.projectId.toString() === filters.project);
          
        return matchesSearch && matchesProject;
      });
      
      // Check if current page would be empty after deletion
      const totalPages = Math.ceil(filteredAnalyses.length / rowsPerPage);
      console.log(`Current page: ${page}, Total pages after deletion: ${totalPages}`);
      
      // If we're on a page that would now be empty (except page 1) and there are other pages
      if (page > totalPages && page > 1) {
        console.log(`Moving to page ${totalPages} because current page ${page} would be empty`);
        setPage(totalPages);
      }
      
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
      setSnackbar({
        open: true,
        message: `Error deleting analysis: ${errorMsg}. Please try again or contact support.`,
        severity: 'error'
      });
    } finally {
      // Reset the delete state
      setConfirmDelete(false);
      setAnalysisToDelete(null);
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
        Analysis History
      </Typography>
      
      <Paper sx={{ p: 3, mb: 4 }}>
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2, mb: 3 }}>
          <TextField
            placeholder="Search analyses..."
            size="small"
            fullWidth
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
            sx={{ flexGrow: 1 }}
          />
          
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel id="project-filter-label">Project</InputLabel>
              <Select
                labelId="project-filter-label"
                name="project"
                value={filters.project}
                label="Project"
                onChange={handleFilterChange}
              >
                {projectOptions.map((option) => (
                  <MenuItem key={option.id} value={option.id}>
                    {option.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </Box>
        
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Project</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Dependencies</TableCell>
                <TableCell>Outdated</TableCell>
                <TableCell>Up to Date</TableCell>
                <TableCell>Vulnerabilities</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {paginatedAnalyses.length > 0 ? (
                paginatedAnalyses.map((analysis) => (
                  <TableRow key={analysis.id} hover>
                    <TableCell>{analysis.id}</TableCell>
                    <TableCell>
                      <Typography
                        variant="body2"
                        component={RouterLink}
                        to={`/projects/${analysis.projectId}`}
                        sx={{ textDecoration: 'none', color: 'primary.main', fontWeight: 'medium' }}
                      >
                        {analysis.projectName}
                      </Typography>
                    </TableCell>
                    <TableCell>{analysis.date}</TableCell>
                    <TableCell>{analysis.totalDependencies || 0}</TableCell>
                    <TableCell>
                      <Typography color={analysis.outdatedDependencies > 0 ? 'warning.main' : 'text.primary'}>
                        {analysis.outdatedDependencies || 0}
                      </Typography>
                    </TableCell>
                    <TableCell>{analysis.upToDateDependencies || 0}</TableCell>
                    <TableCell>
                      <Typography color={analysis.vulnerableCount > 0 ? 'error.main' : 'text.primary'}>
                        {analysis.vulnerableCount || 0}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <IconButton
                        size="small"
                        onClick={() => navigate(`/analysis/${analysis.id}`)}
                        title="View Analysis Details"
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                      
                      <Tooltip title="Download Analysis Report">
                        <span>
                          <IconButton 
                            size="small"
                            onClick={() => handleDownloadReport(analysis.id)}
                            disabled={reportGenerating[analysis.id]}
                          >
                            {reportGenerating[analysis.id] ? (
                              <CircularProgress size={20} />
                            ) : (
                              <DownloadIcon fontSize="small" />
                            )}
                          </IconButton>
                        </span>
                      </Tooltip>
                      
                      <Tooltip title="Delete Analysis">
                        <IconButton 
                          size="small" 
                          color="error"
                          onClick={() => {
                            console.log('Delete button clicked for analysis:', analysis.id);
                            setAnalysisToDelete(analysis.id);
                            setConfirmDelete(true);
                          }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    No analyses found
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
          <Pagination
            count={Math.ceil(filteredAnalyses.length / rowsPerPage)}
            page={page}
            onChange={handlePageChange}
            color="primary"
          />
        </Box>
      </Paper>
      
      <Box sx={{ mb: 4 }}>
        <Typography variant="h6" gutterBottom>
          Stats
        </Typography>
        
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Total Analyses
                </Typography>
                <Typography variant="h4">
                  {analyses.length}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Total Dependencies
                </Typography>
                <Typography variant="h4">
                  {analyses.reduce((sum, a) => sum + (a.totalDependencies || 0), 0)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Outdated Dependencies
                </Typography>
                <Typography variant="h4" color="warning.main">
                  {analyses.reduce((sum, a) => sum + (a.outdatedDependencies || 0), 0)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Vulnerabilities
                </Typography>
                <Typography variant="h4" color="error.main">
                  {analyses.reduce((sum, a) => sum + (a.vulnerableCount || 0), 0)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
      
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

export default AnalysisHistory; 