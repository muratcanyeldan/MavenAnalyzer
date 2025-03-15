import React, { useState } from 'react';
import { 
  Typography, 
  IconButton,
  Popover,
  Box,
  Paper
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';

/**
 * Component for displaying BOM-managed dependency versions
 * 
 * @param {Object} props - Component props
 * @param {string} props.version - The version string (may include "MANAGED_BY_BOM")
 * @param {string} props.groupId - The Maven group ID
 * @param {string} props.artifactId - The Maven artifact ID
 * @param {string} props.estimatedVersion - Optional estimated version (for backward compatibility)
 */
const BomManagedVersion = ({ version, groupId, artifactId, estimatedVersion }) => {
  const [anchorEl, setAnchorEl] = useState(null);
  
  const isBomManaged = version && version.includes('MANAGED_BY_BOM');
  const hasEstimatedVersion = isBomManaged && estimatedVersion;

  // Extract BOM information if available
  const bomInfo = isBomManaged && version.includes('(') && version.includes(')') 
    ? version.match(/\((.*?)\)/)[1] 
    : null;
  
  // If not BOM managed, just display the version as-is
  if (!isBomManaged) {
    return <Typography>{version}</Typography>;
  }
  
  const handleInfoClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  
  const handleInfoClose = () => {
    setAnchorEl(null);
  };
  
  const popoverOpen = Boolean(anchorEl);
  const popoverId = popoverOpen ? 'bom-version-popover' : undefined;
  
  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <Typography 
          sx={{ 
            fontStyle: 'italic',
            color: 'text.primary'
          }}
        >
          {version}
        </Typography>
        
        <IconButton 
          size="small" 
          onClick={handleInfoClick}
          sx={{ ml: 0.5 }}
          aria-describedby={popoverId}
        >
          <InfoIcon fontSize="small" color="action" />
        </IconButton>
      </Box>
      
      <Popover
        id={popoverId}
        open={popoverOpen}
        anchorEl={anchorEl}
        onClose={handleInfoClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
      >
        <Paper sx={{ p: 2, maxWidth: 400 }}>
          <Typography variant="subtitle2" gutterBottom>
            Dependency Version Information
          </Typography>
          
          <Typography variant="body2" paragraph>
            This dependency is managed by a Bill of Materials (BOM), which means its version
            is defined in a parent POM or dependency management section rather than being explicitly specified.
          </Typography>
          
          {bomInfo && (
            <Typography variant="body2">
              <strong>BOM information:</strong> {bomInfo}
            </Typography>
          )}
          
          {hasEstimatedVersion && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              <strong>Estimated version:</strong> {estimatedVersion}
              <br />
              <em>(Based on POM resolution)</em>
            </Typography>
          )}
        </Paper>
      </Popover>
    </>
  );
};

export default BomManagedVersion; 