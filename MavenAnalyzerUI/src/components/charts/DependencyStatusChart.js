import React, { useMemo } from 'react';
import { ResponsivePie } from '@nivo/pie';
import { Box, Typography, CircularProgress } from '@mui/material';

/**
 * Dependency Status Pie Chart component using Nivo
 * 
 * @param {Object} props - Component props
 * @param {Object} props.data - Either analysis data object or pie chart data from API
 * @param {boolean} props.loading - Loading state
 * @param {number} props.height - Chart height in pixels
 */
const DependencyStatusChart = ({ data, loading, height = 300 }) => {
  // Prepare data for the pie chart - moved to top level for Hook rule compliance
  const chartData = useMemo(() => {
    // Check if we have the new API data format (array of entries)
    if (data?.data && Array.isArray(data.data)) {
      // We have data from the chart data API
      return data.data;
    }
    
    // Fallback to the old format (analysis object)
    if (!data || !data.upToDateDependencies) {
      return [];
    }

    return [
      {
        id: 'Up-to-date',
        label: 'Up-to-date',
        value: data.upToDateDependencies || 0,
        color: '#4caf50' // green
      },
      {
        id: 'Outdated',
        label: 'Outdated',
        value: data.outdatedDependencies || 0,
        color: '#ff9800' // orange
      },
      {
        id: 'Unknown',
        label: 'Unknown',
        value: data.unidentifiedDependencies || 0,
        color: '#9e9e9e' // grey
      }
    ].filter(item => item.value > 0); // Only include items with values > 0
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
  if ((!data?.data && !data?.upToDateDependencies) || chartData.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height, p: 2 }}>
        <Typography color="text.secondary">No dependency data available</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ height }}>
      <ResponsivePie
        data={chartData}
        margin={{ top: 40, right: 120, bottom: 40, left: 40 }}
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

export default DependencyStatusChart; 