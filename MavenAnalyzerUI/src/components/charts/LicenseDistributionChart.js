import React, { useMemo } from 'react';
import { ResponsivePie } from '@nivo/pie';
import { Box, Typography, CircularProgress } from '@mui/material';

/**
 * License Distribution Pie Chart component using Nivo
 * 
 * @param {Object} props - Component props
 * @param {Object} props.data - Either analysis data object or pie chart data from API
 * @param {boolean} props.loading - Loading state
 * @param {number} props.height - Chart height in pixels
 * @param {boolean} props.showLegend - Whether to show the legend
 */
const LicenseDistributionChart = ({ data, loading, height = 300, showLegend = true }) => {
  // Prepare data for the pie chart - moved to top level for Hook rule compliance
  const chartData = useMemo(() => {
    // Check if we have the new API data format (array of entries)
    if (data?.data && Array.isArray(data.data)) {
      // We have data from the chart data API
      return data.data;
    }
    
    // Fallback to the old format (analysis object with dependencies)
    if (!data || !data.dependencies || !Array.isArray(data.dependencies)) {
      return [];
    }

    // Count licenses
    const licenseCounts = {};
    data.dependencies.forEach(dependency => {
      const license = dependency.license || 'Unknown';
      licenseCounts[license] = (licenseCounts[license] || 0) + 1;
    });

    // Convert to chart format
    return Object.entries(licenseCounts)
      .filter(([_, count]) => count > 0) // Only include non-zero values
      .map(([license, count]) => {
        // Generate a consistent color based on the license name (simple hash)
        const hash = license.split('').reduce((acc, char) => {
          return char.charCodeAt(0) + ((acc << 5) - acc);
        }, 0);
        const color = `hsl(${Math.abs(hash) % 360}, 70%, 50%)`;
        
        return {
          id: license,
          label: license,
          value: count,
          color: color
        };
      });
  }, [data]);

  // Return loading indicator if data is loading
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height }}>
        <CircularProgress />
      </Box>
    );
  }

  // Return message if no data available
  if (chartData.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height, p: 2 }}>
        <Typography color="text.secondary">No license data available</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ height }}>
      <ResponsivePie
        data={chartData}
        margin={{ top: 40, right: showLegend ? 120 : 40, bottom: 40, left: 40 }}
        innerRadius={0.5}
        padAngle={0.7}
        cornerRadius={3}
        activeOuterRadiusOffset={8}
        borderWidth={1}
        borderColor={{ from: 'color', modifiers: [['darker', 0.2]] }}
        arcLinkLabelsSkipAngle={10}
        arcLinkLabelsTextColor="#333333"
        arcLinkLabelsThickness={2}
        arcLinkLabelsColor={{ from: 'color' }}
        arcLabelsSkipAngle={10}
        arcLabelsTextColor={{ from: 'color', modifiers: [['darker', 2]] }}
        enableArcLabels={false}
        colors={{ datum: 'data.color' }}
        legends={showLegend ? [
          {
            anchor: 'right',
            direction: 'column',
            justify: false,
            translateX: 30,
            translateY: 0,
            itemsSpacing: 5,
            itemWidth: 85,
            itemHeight: 20,
            itemTextColor: '#666',
            itemDirection: 'left-to-right',
            itemOpacity: 1,
            symbolSize: 15,
            symbolShape: 'circle'
          }
        ] : []}
      />
    </Box>
  );
};

export default LicenseDistributionChart; 