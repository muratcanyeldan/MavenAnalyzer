import React, { useState } from 'react';
import { 
  Box, 
  Container, 
  Typography, 
  Link, 
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  TextField,
  IconButton,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Alert
} from '@mui/material';
import { 
  Description as DocumentationIcon, 
  Email as EmailIcon,
  Close as CloseIcon,
  ArrowForward as ArrowForwardIcon,
  Settings as SettingsIcon,
  Info as InfoIcon,
  HelpOutline as HelpIcon,
  Build as BuildIcon,
  Security as SecurityIcon
} from '@mui/icons-material';

const Footer = () => {
  const [docDialogOpen, setDocDialogOpen] = useState(false);
  const [supportDialogOpen, setSupportDialogOpen] = useState(false);
  const [contactForm, setContactForm] = useState({
    name: '',
    email: '',
    subject: '',
    message: ''
  });
  const [formSubmitted, setFormSubmitted] = useState(false);

  const handleDocDialogOpen = () => {
    setDocDialogOpen(true);
  };

  const handleDocDialogClose = () => {
    setDocDialogOpen(false);
  };

  const handleSupportDialogOpen = () => {
    setSupportDialogOpen(true);
    setFormSubmitted(false);
  };

  const handleSupportDialogClose = () => {
    setSupportDialogOpen(false);
    // Reset form when closing
    setContactForm({
      name: '',
      email: '',
      subject: '',
      message: ''
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setContactForm({
      ...contactForm,
      [name]: value
    });
  };

  const handleSubmitForm = (e) => {
    e.preventDefault();
    console.log('Support form submitted:', contactForm);
    // In a real app, you would send this data to your backend
    // For now, we'll just simulate a successful submission
    setFormSubmitted(true);
    
    // Optional: Send an email directly (only works if user has email client configured)
    window.open(`mailto:support@mavenanalyzer.com?subject=${encodeURIComponent(contactForm.subject)}&body=${encodeURIComponent(`Name: ${contactForm.name}\nEmail: ${contactForm.email}\n\n${contactForm.message}`)}`);
  };

  return (
    <Box
      component="footer"
      sx={{
        py: 3,
        px: 2,
        mt: 'auto',
        backgroundColor: (theme) => 
          theme.palette.mode === 'light' ? theme.palette.grey[100] : theme.palette.grey[900],
      }}
    >
      <Container maxWidth="lg">
        <Typography variant="body2" color="text.secondary" align="center">
          {'© '}
          {new Date().getFullYear()}
          {' '}
          <Link color="inherit" href="#">
            Maven Dependency Analyzer
          </Link>
          {' | '}
          <Link 
            color="inherit" 
            href="#" 
            onClick={(e) => {
              e.preventDefault();
              handleDocDialogOpen();
            }}
          >
            Documentation
          </Link>
          {' | '}
          <Link 
            color="inherit" 
            href="#" 
            onClick={(e) => {
              e.preventDefault();
              handleSupportDialogOpen();
            }}
          >
            Support
          </Link>
        </Typography>
      </Container>

      {/* Documentation Dialog */}
      <Dialog
        open={docDialogOpen}
        onClose={handleDocDialogClose}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Grid container alignItems="center" justifyContent="space-between">
            <Grid item>
              <Box display="flex" alignItems="center">
                <DocumentationIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Maven Dependency Analyzer Documentation</Typography>
              </Box>
            </Grid>
            <Grid item>
              <IconButton onClick={handleDocDialogClose} edge="end">
                <CloseIcon />
              </IconButton>
            </Grid>
          </Grid>
        </DialogTitle>
        <Divider />
        <DialogContent>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <Typography variant="h6" gutterBottom>
                Quick Guide
              </Typography>
              <List>
                <ListItem 
                  button 
                  onClick={() => {
                    // Open specific section in the dialog
                    const gettingStartedSection = document.getElementById('getting-started-section');
                    if (gettingStartedSection) {
                      gettingStartedSection.scrollIntoView({ behavior: 'smooth' });
                    }
                  }}
                >
                  <ListItemIcon>
                    <InfoIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Getting Started" />
                </ListItem>
                <ListItem 
                  button 
                  onClick={() => {
                    // Open specific section in the dialog
                    const projectsSection = document.getElementById('projects-section');
                    if (projectsSection) {
                      projectsSection.scrollIntoView({ behavior: 'smooth' });
                    }
                  }}
                >
                  <ListItemIcon>
                    <BuildIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Managing Projects" />
                </ListItem>
                <ListItem 
                  button 
                  onClick={() => {
                    // Open specific section in the dialog
                    const securitySection = document.getElementById('security-section');
                    if (securitySection) {
                      securitySection.scrollIntoView({ behavior: 'smooth' });
                    }
                  }}
                >
                  <ListItemIcon>
                    <SecurityIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Security Analysis" />
                </ListItem>
                <ListItem 
                  button 
                  onClick={() => {
                    // Open specific section in the dialog
                    const configSection = document.getElementById('config-section');
                    if (configSection) {
                      configSection.scrollIntoView({ behavior: 'smooth' });
                    }
                  }}
                >
                  <ListItemIcon>
                    <SettingsIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="Configuration" />
                </ListItem>
                <ListItem 
                  button 
                  onClick={() => {
                    // Open specific section in the dialog
                    const faqSection = document.getElementById('faq-section');
                    if (faqSection) {
                      faqSection.scrollIntoView({ behavior: 'smooth' });
                    }
                  }}
                >
                  <ListItemIcon>
                    <HelpIcon color="primary" />
                  </ListItemIcon>
                  <ListItemText primary="FAQ" />
                </ListItem>
              </List>
            </Grid>
            <Grid item xs={12} md={8}>
              <Typography variant="h6" gutterBottom>
                About Maven Dependency Analyzer
              </Typography>
              <Typography paragraph>
                Maven Dependency Analyzer is a powerful tool designed to help developers manage and analyze their Maven project dependencies.
                It provides insights into outdated dependencies, security vulnerabilities, and license compliance issues.
              </Typography>
              <Typography paragraph>
                The tool scans your Maven POM files, analyzes the dependencies, and provides recommendations for updates and security patches.
                It also generates reports that can be shared with your team or stakeholders.
              </Typography>
              <Box mt={4}>
                <Typography variant="h6" gutterBottom id="getting-started-section">
                  Getting Started
                </Typography>
                <Typography paragraph>
                  To get started with Maven Dependency Analyzer, follow these simple steps:
                </Typography>
                <ol>
                  <li>
                    <Typography paragraph>
                      <strong>Create a Project:</strong> Navigate to the Projects section and click "Create New Project" to set up your Maven project.
                    </Typography>
                  </li>
                  <li>
                    <Typography paragraph>
                      <strong>Upload POM File:</strong> Upload your project's pom.xml file to start analyzing dependencies.
                    </Typography>
                  </li>
                  <li>
                    <Typography paragraph>
                      <strong>Run Analysis:</strong> Click "Run Analysis" to scan your dependencies for updates, vulnerabilities, and license issues.
                    </Typography>
                  </li>
                  <li>
                    <Typography paragraph>
                      <strong>Review Results:</strong> Check the analysis results for outdated dependencies, security vulnerabilities, and license compliance issues.
                    </Typography>
                  </li>
                </ol>
                
                <Typography variant="h6" gutterBottom id="projects-section" mt={3}>
                  Managing Projects
                </Typography>
                <Typography paragraph>
                  The Projects section allows you to organize and manage multiple Maven projects in one place. You can:
                </Typography>
                <ul>
                  <li>
                    <Typography>Create, edit, and delete projects</Typography>
                  </li>
                  <li>
                    <Typography>View project details including dependencies and analysis history</Typography>
                  </li>
                  <li>
                    <Typography>Set project status (active/inactive)</Typography>
                  </li>
                  <li>
                    <Typography>Run new analyses on existing projects</Typography>
                  </li>
                </ul>
                
                <Typography variant="h6" gutterBottom id="security-section" mt={3}>
                  Security Analysis
                </Typography>
                <Typography paragraph>
                  Maven Dependency Analyzer checks your dependencies against vulnerability databases to identify security risks:
                </Typography>
                <ul>
                  <li>
                    <Typography>Vulnerabilities are categorized by severity (Critical, High, Medium, Low)</Typography>
                  </li>
                  <li>
                    <Typography>Detailed information about each vulnerability is provided</Typography>
                  </li>
                  <li>
                    <Typography>Recommended fixes or version updates are suggested</Typography>
                  </li>
                  <li>
                    <Typography>Scan results can be exported for compliance documentation</Typography>
                  </li>
                </ul>
                
                <Typography variant="h6" gutterBottom id="config-section" mt={3}>
                  Configuration
                </Typography>
                <Typography paragraph>
                  Customize the analyzer through the Settings page:
                </Typography>
                <ul>
                  <li>
                    <Typography>Set restricted licenses to flag in analyses</Typography>
                  </li>
                  <li>
                    <Typography>Configure vulnerability scanning options</Typography>
                  </li>
                  <li>
                    <Typography>Manage cache settings for faster performance</Typography>
                  </li>
                  <li>
                    <Typography>Set up notification preferences</Typography>
                  </li>
                </ul>
                
                <Typography variant="h6" gutterBottom id="faq-section" mt={3}>
                  FAQ
                </Typography>
                <Typography variant="subtitle1" gutterBottom>
                  How often should I run a dependency analysis?
                </Typography>
                <Typography paragraph>
                  We recommend running an analysis at least monthly, or whenever adding new dependencies to your project.
                </Typography>
                
                <Typography variant="subtitle1" gutterBottom>
                  Can I automate dependency analysis?
                </Typography>
                <Typography paragraph>
                  Yes! You can integrate Maven Dependency Analyzer into your CI/CD pipeline using our REST API.
                </Typography>
                
                <Typography variant="subtitle1" gutterBottom>
                  How are vulnerabilities identified?
                </Typography>
                <Typography paragraph>
                  We check your dependencies against multiple vulnerability databases including the National Vulnerability Database (NVD).
                </Typography>
                
                <Typography variant="subtitle1" gutterBottom>
                  Can I export the analysis results?
                </Typography>
                <Typography paragraph>
                  Yes, you can generate and download comprehensive reports in various formats, including PDF and JSON.
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={handleDocDialogClose} 
            color="primary"
          >
            Close
          </Button>
          <Button 
            variant="contained" 
            endIcon={<ArrowForwardIcon />}
            onClick={() => {
              window.open("https://github.com/muratcanyeldan/MavenDependencyAnalyzer", "_blank");
              handleDocDialogClose();
            }}
          >
            Full Documentation
          </Button>
        </DialogActions>
      </Dialog>

      {/* Support Dialog */}
      <Dialog
        open={supportDialogOpen}
        onClose={handleSupportDialogClose}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          <Grid container alignItems="center" justifyContent="space-between">
            <Grid item>
              <Box display="flex" alignItems="center">
                <EmailIcon sx={{ mr: 1 }} />
                <Typography variant="h6">Contact Support</Typography>
              </Box>
            </Grid>
            <Grid item>
              <IconButton onClick={handleSupportDialogClose} edge="end">
                <CloseIcon />
              </IconButton>
            </Grid>
          </Grid>
        </DialogTitle>
        <Divider />
        <DialogContent>
          {formSubmitted ? (
            <Box mt={2} mb={2}>
              <Alert severity="success">
                Thank you for contacting support! We'll get back to you as soon as possible.
              </Alert>
            </Box>
          ) : (
            <form onSubmit={handleSubmitForm}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="name"
                    label="Your Name"
                    fullWidth
                    required
                    value={contactForm.name}
                    onChange={handleInputChange}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="email"
                    label="Email Address"
                    fullWidth
                    required
                    type="email"
                    value={contactForm.email}
                    onChange={handleInputChange}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    name="subject"
                    label="Subject"
                    fullWidth
                    required
                    value={contactForm.subject}
                    onChange={handleInputChange}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    name="message"
                    label="Message"
                    fullWidth
                    required
                    multiline
                    rows={4}
                    value={contactForm.message}
                    onChange={handleInputChange}
                  />
                </Grid>
              </Grid>
            </form>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleSupportDialogClose} color="primary">
            Cancel
          </Button>
          {!formSubmitted && (
            <Button 
              type="submit" 
              variant="contained" 
              color="primary" 
              endIcon={<EmailIcon />}
              onClick={handleSubmitForm}
            >
              Send Message
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Footer; 