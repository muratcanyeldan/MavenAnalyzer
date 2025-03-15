// Utility functions for date formatting

/**
 * Formats a date string into a more readable format
 * @param {string} dateString - The date string to format (ISO format)
 * @param {boolean} includeTime - Whether to include time in the formatted date
 * @returns {string} The formatted date string
 */
export const formatDate = (dateString, includeTime = false) => {
  if (!dateString) {
    return 'N/A';
  }
  
  try {
    const date = new Date(dateString);
    if (isNaN(date)) {
      return dateString;
    }
    
    const options = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      ...(includeTime && { hour: '2-digit', minute: '2-digit' })
    };
    
    return date.toLocaleDateString(undefined, options);
  } catch (error) {
    console.error('Error formatting date:', error);
    return dateString;
  }
};

/**
 * Calculates the relative time (e.g., "2 days ago", "just now")
 * @param {string} dateString - The date string to format (ISO format)
 * @returns {string} The relative time string
 */
export const getRelativeTime = (dateString) => {
  if (!dateString) {
    return 'N/A';
  }
  
  try {
    const date = new Date(dateString);
    if (isNaN(date)) {
      return dateString;
    }
    
    const now = new Date();
    const diff = now - date;
    
    // Less than a minute
    if (diff < 60 * 1000) {
      return 'just now';
    }
    
    // Less than an hour
    if (diff < 60 * 60 * 1000) {
      const minutes = Math.floor(diff / (60 * 1000));
      return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
    }
    
    // Less than a day
    if (diff < 24 * 60 * 60 * 1000) {
      const hours = Math.floor(diff / (60 * 60 * 1000));
      return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
    }
    
    // Less than a week
    if (diff < 7 * 24 * 60 * 60 * 1000) {
      const days = Math.floor(diff / (24 * 60 * 60 * 1000));
      return `${days} day${days !== 1 ? 's' : ''} ago`;
    }
    
    // Otherwise, return formatted date
    return formatDate(dateString);
  } catch (error) {
    console.error('Error calculating relative time:', error);
    return dateString;
  }
}; 