import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Typography,
  Box,
  Paper,
  Grid,
  Button,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  IconButton,
  Chip,
  CircularProgress,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Alert,
  Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import HistoryIcon from '@mui/icons-material/History';
import AssessmentIcon from '@mui/icons-material/Assessment';
import WarningIcon from '@mui/icons-material/Warning';
import UpdateIcon from '@mui/icons-material/Update';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import api from '../services/api';

// Tab panel component for tab content
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`project-tabpanel-${index}`}
      aria-labelledby={`project-tab-${index}`}
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

const ProjectDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [project, setProject] = useState(null);
  const [analyses, setAnalyses] = useState([]);
  const [tabValue, setTabValue] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');

  // Helper function to format date strings
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    
    try {
      const date = new Date(dateString);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('Error formatting date:', error);
      return dateString;
    }
  };

  useEffect(() => {
    const fetchProjectData = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // Fetch project details
        const projectResponse = await api.projects.getById(id);
        setProject(projectResponse.data);
        setNewName(projectResponse.data.name);
        setNewDescription(projectResponse.data.description || '');
        
        // Fetch analyses for this project
        try {
          const analysesResponse = await api.dependencyAnalysis.getByProject(id);
          if (analysesResponse.data && analysesResponse.data.length > 0) {
            // Format the date for each analysis and ensure id is properly mapped
            const formattedAnalyses = analysesResponse.data.map(analysis => ({
              ...analysis,
              id: analysis.analysisId, // Map analysisId to id for consistency
              date: formatDate(analysis.analysisDate) // Use the same formatDate function
            }));
            setAnalyses(formattedAnalyses);
          } else {
            setAnalyses([]);
          }
        } catch (analysesError) {
          console.error('Error fetching analyses:', analysesError);
          setAnalyses([]);
        }
      } catch (projectError) {
        console.error('Error fetching project:', projectError);
        setError('Failed to load project data. Please try again.');
        setProject(null);
      } finally {
        setLoading(false);
      }
    };

    fetchProjectData();
  }, [id]);

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleDelete = async () => {
    try {
      setLoading(true);
      await api.projects.delete(id);
      setDeleteDialogOpen(false);
      navigate('/projects', { state: { alert: { type: 'success', message: 'Project deleted successfully' } } });
    } catch (err) {
      console.error('Error deleting project:', err);
      setError({ severity: 'error', message: 'Failed to delete project. Please try again later.' });
      setLoading(false);
    }
  };

  const handleEdit = async () => {
    if (!newName.trim()) return;
    
    try {
      setLoading(true);
      const updatedProject = { 
        ...project, 
        name: newName,
        description: newDescription 
      };
      const response = await api.projects.update(id, updatedProject);
      setProject(response.data);
      setEditDialogOpen(false);
      setLoading(false);
    } catch (err) {
      console.error('Error updating project:', err);
      setError({ severity: 'error', message: 'Failed to update project. Please try again later.' });
      setLoading(false);
    }
  };

  const handleSaveChanges = async () => {
    try {
      setLoading(true);
      const response = await api.projects.update(id, project);
      setProject(response.data);
      setLoading(false);
      // Show a temporary success message
      setError({ severity: 'success', message: 'Project updated successfully' });
      setTimeout(() => setError(null), 3000);
    } catch (err) {
      console.error('Error updating project:', err);
      setError({ severity: 'error', message: 'Failed to save changes. Please try again later.' });
      setLoading(false);
    }
  };

  if (loading && !project) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  // Critical error that prevents rendering the whole component
  if (error && typeof error === 'string') {
    return (
      <Box sx={{ mt: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  return (
    <>
      {error && typeof error === 'object' && (
        <Alert 
          severity={error.severity} 
          sx={{ mb: 2 }}
          onClose={() => setError(null)}
        >
          {error.message}
        </Alert>
      )}
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          {project.name}
        </Typography>
        <Box>
          <Button 
            variant="outlined" 
            startIcon={<EditIcon />} 
            sx={{ mr: 1 }}
            onClick={() => setEditDialogOpen(true)}
          >
            Edit
          </Button>
          <Button 
            variant="outlined" 
            color="error" 
            startIcon={<DeleteIcon />}
            onClick={() => setDeleteDialogOpen(true)}
          >
            Delete
          </Button>
        </Box>
      </Box>
      
      <Paper sx={{ p: 3, mb: 4 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={8}>
            <Typography variant="body1" paragraph>
              {project.description || 'No description provided.'}
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              <Chip label="Type: MAVEN" variant="outlined" />
              <Chip label={`Created: ${formatDate(project.createdAt)}`} variant="outlined" />
              <Chip label={`Last Analysis: ${formatDate(project.lastAnalysisDate)}`} variant="outlined" />
            </Box>
          </Grid>
          <Grid item xs={12} md={4} sx={{ display: 'flex', justifyContent: { xs: 'flex-start', md: 'flex-end' }, alignItems: 'flex-start' }}>
            <Tooltip title={project.status === 'INACTIVE' ? "Cannot create analysis for inactive project" : ""}>
              <span>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => navigate(`/projects/${id}/new-analysis`)}
                  size="large"
                  disabled={project.status === 'INACTIVE'}
                >
                  New Analysis
                </Button>
              </span>
            </Tooltip>
          </Grid>
        </Grid>
      </Paper>
      
      <Paper sx={{ mb: 4 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs 
            value={tabValue} 
            onChange={handleTabChange} 
            aria-label="project tabs"
          >
            <Tab label="Overview" />
            <Tab label="Analysis History" />
            <Tab label="Settings" />
          </Tabs>
        </Box>
        
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader 
                  title="Latest Analysis" 
                  action={
                    analyses.length > 0 ? (
                      <Button 
                        component={RouterLink} 
                        to={`/analysis/${analyses[0].id}`} 
                        size="small" 
                        endIcon={<ArrowForwardIcon />}
                      >
                        View Details
                      </Button>
                    ) : (
                      <Tooltip title={project.status === 'INACTIVE' ? "Cannot create analysis for inactive project" : ""}>
                        <span>
                          <Button 
                            component={RouterLink} 
                            to={`/projects/${id}/new-analysis`} 
                            size="small" 
                            startIcon={<AddIcon />}
                            disabled={project.status === 'INACTIVE'}
                          >
                            New Analysis
                          </Button>
                        </span>
                      </Tooltip>
                    )
                  }
                />
                <Divider />
                <CardContent>
                  {analyses.length > 0 ? (
                    <>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        Analyzed on {analyses[0].date}
                      </Typography>
                      
                      <Grid container spacing={2} sx={{ mt: 1 }}>
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: 'center' }}>
                            <Typography variant="h5">
                              {analyses[0].totalDependencies}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              Dependencies
                            </Typography>
                          </Box>
                        </Grid>
                        
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: 'center' }}>
                            <Typography variant="h5" color="warning.main">
                              {analyses[0].outdatedDependencies}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              Outdated
                            </Typography>
                          </Box>
                        </Grid>
                        
                        <Grid item xs={4}>
                          <Box sx={{ textAlign: 'center' }}>
                            <Typography variant="h5" color="error.main">
                              {analyses[0].vulnerableCount || 0}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              Vulnerabilities
                            </Typography>
                          </Box>
                        </Grid>
                      </Grid>
                    </>
                  ) : (
                    <Box sx={{ textAlign: 'center', py: 3 }}>
                      <Typography variant="body1" gutterBottom>
                        No analyses found for this project.
                      </Typography>
                      <Tooltip title={project.status === 'INACTIVE' ? "Cannot create analysis for inactive project" : ""}>
                        <span>
                          <Button
                            variant="contained"
                            startIcon={<AddIcon />}
                            onClick={() => navigate(`/projects/${id}/new-analysis`)}
                            sx={{ mt: 2 }}
                            disabled={project.status === 'INACTIVE'}
                          >
                            Start First Analysis
                          </Button>
                        </span>
                      </Tooltip>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader title="Project Summary" />
                <Divider />
                <CardContent>
                  <List dense>
                    <ListItem>
                      <ListItemIcon>
                        <AssessmentIcon />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Total Analyses" 
                        secondary={analyses.length}
                      />
                    </ListItem>
                    
                    <ListItem>
                      <ListItemIcon>
                        <HistoryIcon />
                      </ListItemIcon>
                      <ListItemText 
                        primary="Last Analysis" 
                        secondary={analyses.length > 0 ? analyses[0].date : 'Never'}
                      />
                    </ListItem>
                    
                    {analyses.length > 0 && (
                      <>
                        <ListItem>
                          <ListItemIcon>
                            <UpdateIcon />
                          </ListItemIcon>
                          <ListItemText 
                            primary="Outdated Dependencies" 
                            secondary={`${analyses[0].outdatedDependencies} of ${analyses[0].totalDependencies}`}
                          />
                        </ListItem>
                        
                        <ListItem>
                          <ListItemIcon>
                            <WarningIcon />
                          </ListItemIcon>
                          <ListItemText 
                            primary="Security Vulnerabilities" 
                            secondary={analyses[0].vulnerableCount || 0}
                          />
                        </ListItem>
                      </>
                    )}
                  </List>
                </CardContent>
              </Card>
            </Grid>
            
            {analyses.length > 0 && analyses[0].vulnerableCount > 0 && (
              <Grid item xs={12}>
                <Card sx={{ bgcolor: 'error.lighter', borderLeft: '4px solid', borderColor: 'error.main' }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <WarningIcon color="error" sx={{ fontSize: 40, mr: 2 }} />
                      <Box>
                        <Typography variant="h6" color="error.main" gutterBottom>
                          Security Alert: {analyses[0].vulnerableCount} vulnerabilities detected
                        </Typography>
                        <Typography variant="body2">
                          Your project has dependencies with known security vulnerabilities that should be addressed. 
                          View the latest analysis for details and recommendations.
                        </Typography>
                      </Box>
                      <Box sx={{ ml: 'auto' }}>
                        <Button 
                          variant="contained" 
                          color="error"
                          component={RouterLink} 
                          to={`/analysis/${analyses[0].id}`}
                          endIcon={<ArrowForwardIcon />}
                        >
                          View Vulnerabilities
                        </Button>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>
        </TabPanel>
        
        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Analysis History
          </Typography>
          
          {analyses.length > 0 ? (
            <List>
              {analyses.map((analysis) => (
                <Paper key={analysis.id} sx={{ mb: 2 }}>
                  <ListItem 
                    button 
                    component={RouterLink} 
                    to={`/analysis/${analysis.id}`}
                    sx={{ display: 'block', p: 0 }}
                  >
                    <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Box>
                        <Typography variant="subtitle1">
                          Analysis {analysis.id ? ` ${analysis.id}` : ''}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Completed on {analysis.date}
                        </Typography>
                      </Box>
                      <Box sx={{ display: 'flex', gap: 2 }}>
                        <Chip 
                          label={`${analysis.totalDependencies} Dependencies`}
                          variant="outlined"
                          size="small"
                        />
                        <Chip 
                          label={`${analysis.outdatedDependencies} Outdated`}
                          color="warning"
                          variant="outlined"
                          size="small"
                        />
                        <Chip 
                          label={`${analysis.vulnerableCount || 0} Vulnerabilities`}
                          color="error"
                          variant="outlined"
                          size="small"
                        />
                        <IconButton size="small">
                          <ArrowForwardIcon fontSize="small" />
                        </IconButton>
                      </Box>
                    </Box>
                  </ListItem>
                </Paper>
              ))}
            </List>
          ) : (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Typography variant="body1" gutterBottom>
                No analyses found for this project.
              </Typography>
              <Tooltip title={project.status === 'INACTIVE' ? "Cannot create analysis for inactive project" : ""}>
                <span>
                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={() => navigate(`/projects/${id}/new-analysis`)}
                    sx={{ mt: 2 }}
                    disabled={project.status === 'INACTIVE'}
                  >
                    Start First Analysis
                  </Button>
                </span>
              </Tooltip>
            </Box>
          )}
        </TabPanel>
        
        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" gutterBottom>
            Project Settings
          </Typography>
          
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader title="General Settings" />
                <Divider />
                <CardContent>
                  <TextField
                    label="Project Name"
                    fullWidth
                    value={project.name}
                    variant="outlined"
                    sx={{ mb: 3 }}
                    onChange={(e) => setProject({...project, name: e.target.value})}
                  />
                  
                  <TextField
                    label="Description"
                    fullWidth
                    multiline
                    rows={4}
                    value={project.description}
                    variant="outlined"
                    sx={{ mb: 3 }}
                    onChange={(e) => setProject({...project, description: e.target.value})}
                  />
                  
                  <TextField
                    label="Default POM Directory Path"
                    fullWidth
                    placeholder="Path to POM directory (for resolving BOM versions)"
                    value={project.defaultPomPath || ''}
                    variant="outlined"
                    sx={{ mb: 3 }}
                    onChange={(e) => setProject({...project, defaultPomPath: e.target.value})}
                    helperText="Set a default path where Maven can find and resolve BOM dependencies. This will be pre-filled when starting a new analysis."
                  />
                  
                  <Button variant="contained" onClick={handleSaveChanges}>
                    Save Changes
                  </Button>
                </CardContent>
              </Card>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader 
                  title="Danger Zone" 
                  sx={{ color: 'error.main' }}
                />
                <Divider />
                <CardContent>
                  <Typography variant="body2" paragraph>
                    Deleting this project will remove all associated data, including analyses and reports. 
                    This action cannot be undone.
                  </Typography>
                  
                  <Button 
                    variant="outlined" 
                    color="error" 
                    startIcon={<DeleteIcon />}
                    onClick={() => setDeleteDialogOpen(true)}
                  >
                    Delete Project
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
      </Paper>
      
      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
      >
        <DialogTitle>Delete Project?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete <strong>{project.name}</strong>? This action cannot be undone and all project data, including analyses and reports, will be permanently removed.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleDelete} color="error">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Edit Project Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
      >
        <DialogTitle>Edit Project</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Update project information.
          </DialogContentText>
          <TextField
            autoFocus
            label="Project Name"
            fullWidth
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            margin="normal"
          />
          <TextField
            label="Description"
            fullWidth
            multiline
            rows={4}
            value={newDescription}
            onChange={(e) => setNewDescription(e.target.value)}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleEdit} color="primary" disabled={!newName.trim()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default ProjectDetail; 