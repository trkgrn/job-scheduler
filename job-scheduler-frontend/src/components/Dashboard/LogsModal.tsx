import React, { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Paper,
  Chip,
  IconButton,
  Switch,
  FormControlLabel,
  Tooltip
} from '@mui/material';
import {
  Close as CloseIcon,
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  Description as LogsIcon
} from '@mui/icons-material';
import { LogEntry } from '../../types';

interface LogsModalProps {
  open: boolean;
  onClose: () => void;
  execution: any;
  logs: LogEntry[];
  onRefresh: () => void;
  onDownload: () => void;
}

const LogsModal: React.FC<LogsModalProps> = ({
  open,
  onClose,
  execution,
  logs,
  onRefresh,
  onDownload
}) => {
  const [autoRefresh, setAutoRefresh] = useState(false);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (autoRefresh && open) {
      interval = setInterval(() => {
        onRefresh();
      }, 2000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [autoRefresh, open, onRefresh]);

  const formatDateTime = (dateTime?: string) => {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    const dateStr = date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    const timeStr = date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    const milliseconds = date.getMilliseconds().toString().padStart(3, '0');
    const timeWithMs = `${timeStr}.${milliseconds}`;
    return { date: dateStr, time: timeWithMs };
  };

  const getLogLevelColor = (level: string) => {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return 'error';
      case 'WARN':
        return 'warning';
      case 'INFO':
        return 'info';
      case 'DEBUG':
        return 'default';
      default:
        return 'default';
    }
  };

  const handleDownload = () => {
    const logText = logs.map(log => 
      `[${log.timestamp}] ${log.level}: ${log.message}`
    ).join('\n');
    
    const blob = new Blob([logText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `execution-${execution?.id}-logs.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="lg" 
      fullWidth
      fullScreen={window.innerWidth < 600 ? true : false}
      sx={{
        '& .MuiDialog-paper': {
          margin: { xs: 0, sm: 32 },
          maxHeight: { xs: '100vh', sm: '90vh' }
        }
      }}
    >
      <DialogTitle>
        <Box 
          display="flex" 
          alignItems="center" 
          justifyContent="space-between"
          flexDirection={{ xs: 'column', sm: 'row' }}
          gap={{ xs: 2, sm: 0 }}
        >
          <Box display="flex" alignItems="center" gap={1}>
            <LogsIcon />
            <Typography variant="h6" sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}>
              Execution Logs {execution && `#${execution.id}`}
            </Typography>
          </Box>
          <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
            <FormControlLabel
              control={
                <Switch
                  checked={autoRefresh}
                  onChange={(e) => setAutoRefresh(e.target.checked)}
                  size="small"
                />
              }
              label="Auto Refresh"
            />
            <Tooltip title="Refresh Logs">
              <IconButton onClick={onRefresh} size="small">
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Download Logs">
              <IconButton onClick={handleDownload} size="small">
                <DownloadIcon />
              </IconButton>
            </Tooltip>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        {logs.length === 0 ? (
          <Box textAlign="center" py={4}>
            <Typography variant="body1" color="textSecondary">
              No logs available for this execution
            </Typography>
          </Box>
        ) : (
          <Paper sx={{ maxHeight: '70vh', overflow: 'auto', mt: 1 }}>
            {logs.map((log, index) => (
              <Box key={index} sx={{ p: 2, borderBottom: '1px solid #eee' }}>
                <Box display="flex" alignItems="center" gap={1} mb={1}>
                  {(() => {
                    const dateTime = formatDateTime(log.timestamp);
                    if (dateTime === '-') return <Typography variant="caption" color="textSecondary">-</Typography>;
                    if (typeof dateTime === 'object') {
                      return (
                        <Box>
                          <Typography variant="caption" fontWeight="medium" color="textSecondary">
                            {dateTime.date}
                          </Typography>
                          <Typography variant="caption" color="textSecondary" sx={{ display: 'block' }}>
                            {dateTime.time}
                          </Typography>
                        </Box>
                      );
                    }
                    return <Typography variant="caption" color="textSecondary">-</Typography>;
                  })()}
                  <Chip
                    label={log.level}
                    color={getLogLevelColor(log.level) as any}
                    size="small"
                  />
                </Box>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
                  {log.message}
                </Typography>
              </Box>
            ))}
          </Paper>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};

export default LogsModal;
