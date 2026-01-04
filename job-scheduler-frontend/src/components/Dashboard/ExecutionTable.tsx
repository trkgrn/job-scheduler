import React from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Chip,
  TablePagination,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material';
import {
  Visibility as ViewLogsIcon,
  Stop as StopIcon
} from '@mui/icons-material';
import { JobExecution } from '../../types';

interface ExecutionTableProps {
  executions: JobExecution[];
  page: number;
  rowsPerPage: number;
  totalCount: number;
  filterStatus: string;
  isAbortable?: boolean;
  onPageChange: (event: unknown, newPage: number) => void;
  onRowsPerPageChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onStatusFilterChange: (status: string) => void;
  onViewLogs: (execution: JobExecution) => void;
  onAbort?: (execution: JobExecution) => void;
}

const ExecutionTable: React.FC<ExecutionTableProps> = ({
  executions,
  page,
  rowsPerPage,
  totalCount,
  filterStatus,
  isAbortable = false,
  onPageChange,
  onRowsPerPageChange,
  onStatusFilterChange,
  onViewLogs,
  onAbort
}) => {
  // Debug: Log abortable status
  React.useEffect(() => {
    console.log('ExecutionTable - isAbortable:', isAbortable);
    console.log('ExecutionTable - executions count:', executions.length);
    console.log('ExecutionTable - running executions:', executions.filter(e => e.status === 'RUNNING').length);
  }, [isAbortable, executions]);
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

  const formatDuration = (milliseconds: number | null | undefined) => {
    if (!milliseconds) return '-';
    
    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
      return `${days}d ${hours % 24}h ${minutes % 60}m`;
    } else if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'success';
      case 'FAILED':
        return 'error';
      case 'RUNNING':
        return 'info';
      case 'PENDING':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getLogLevelColor = (level?: string) => {
    switch (level?.toUpperCase()) {
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

  return (
    <Box>
      <Box 
        display="flex" 
        justifyContent="space-between" 
        alignItems="center" 
        mb={2}
        flexDirection={{ xs: 'column', sm: 'row' }}
        gap={{ xs: 2, sm: 0 }}
      >
        <Typography variant="h6" sx={{ fontSize: { xs: '1.1rem', sm: '1.25rem' } }}>
          Executions
        </Typography>
        <FormControl size="small" sx={{ minWidth: { xs: '100%', sm: 120 } }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={filterStatus}
            label="Status"
            onChange={(e) => onStatusFilterChange(e.target.value)}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="SUCCESS">Success</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
            <MenuItem value="RUNNING">Running</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer 
        component={Paper}
        sx={{ 
          overflowX: 'auto',
          '&::-webkit-scrollbar': {
            height: 8,
          },
          '&::-webkit-scrollbar-track': {
            backgroundColor: 'grey.100',
          },
          '&::-webkit-scrollbar-thumb': {
            backgroundColor: 'grey.400',
            borderRadius: 4,
          },
        }}
      >
        <Table sx={{ minWidth: { xs: 800, sm: 'auto' } }}>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>ID</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Status</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Start Time</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>End Time</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Duration</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Correlation ID</TableCell>
              <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Log Level</TableCell>
              <TableCell align="center" sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {executions.map((execution) => (
              <TableRow key={execution.id} hover>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Typography variant="body2" fontWeight="medium" sx={{ fontSize: 'inherit' }}>
                    #{execution.id}
                  </Typography>
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Chip
                      label={execution.status || 'UNKNOWN'}
                      color={getStatusColor(execution.status) as any}
                      size="small"
                      sx={{ 
                        fontSize: { xs: '0.65rem', sm: '0.75rem' },
                        height: { xs: 20, sm: 24 }
                      }}
                    />
                  </Box>
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  {(() => {
                    const dateTime = formatDateTime(execution.startTime || execution.startedAt);
                    if (dateTime === '-') return '-';
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium" sx={{ fontSize: 'inherit' }}>
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary" sx={{ fontSize: '0.7rem' }}>
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  {(() => {
                    const dateTime = formatDateTime(execution.endTime || execution.endedAt);
                    if (dateTime === '-') return '-';
                    return (
                      <Box>
                        <Typography variant="body2" fontWeight="medium" sx={{ fontSize: 'inherit' }}>
                          {dateTime.date}
                        </Typography>
                        <Typography variant="caption" color="textSecondary" sx={{ fontSize: '0.7rem' }}>
                          {dateTime.time}
                        </Typography>
                      </Box>
                    );
                  })()}
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Typography sx={{ fontSize: 'inherit' }}>
                    {formatDuration(execution.duration)}
                  </Typography>
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Typography 
                    variant="body2" 
                    sx={{ 
                      fontFamily: 'monospace',
                      fontSize: 'inherit',
                      wordBreak: 'break-all',
                      maxWidth: { xs: '100px', sm: '150px' }
                    }}
                    title={execution.correlationId || 'N/A'}
                  >
                    {execution.correlationId || 'N/A'}
                  </Typography>
                </TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Chip
                    label={execution.logLevel || 'N/A'}
                    color={getLogLevelColor(execution.logLevel) as any}
                    size="small"
                    variant="outlined"
                    sx={{ 
                      fontSize: { xs: '0.65rem', sm: '0.75rem' },
                      height: { xs: 20, sm: 24 }
                    }}
                  />
                </TableCell>
                <TableCell align="center" sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                  <Box display="flex" gap={0.5} justifyContent="center">
                    <IconButton
                      size="small"
                      onClick={() => onViewLogs(execution)}
                      title="View Logs"
                      sx={{ padding: { xs: '4px', sm: '8px' } }}
                    >
                      <ViewLogsIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => onAbort && onAbort(execution)}
                      disabled={!isAbortable || execution.status !== 'RUNNING'}
                      title={
                        !isAbortable
                          ? 'Job is not abortable'
                          : execution.status === 'RUNNING'
                          ? 'Abort Execution'
                          : 'Job is abortable but not currently running'
                      }
                      color={isAbortable && execution.status === 'RUNNING' ? 'error' : 'default'}
                      sx={{ 
                        padding: { xs: '4px', sm: '8px' },
                        opacity: isAbortable && execution.status === 'RUNNING' ? 1 : 0.5
                      }}
                    >
                      <StopIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={totalCount}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={onPageChange}
        onRowsPerPageChange={onRowsPerPageChange}
      />
    </Box>
  );
};

export default ExecutionTable;
