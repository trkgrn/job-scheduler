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
  Button,
  Alert
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon
} from '@mui/icons-material';
import { TriggerModel } from '../../types';

interface TriggerTableProps {
  triggers: TriggerModel[];
  onAddTrigger: () => void;
  onEditTrigger: (trigger: TriggerModel) => void;
  onDeleteTrigger: (triggerId: number) => void;
}

const TriggerTable: React.FC<TriggerTableProps> = ({
  triggers,
  onAddTrigger,
  onEditTrigger,
  onDeleteTrigger
}) => {
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
          Triggers
        </Typography>
        <Button
          variant="outlined"
          startIcon={<AddIcon />}
          size="small"
          onClick={onAddTrigger}
          sx={{ minWidth: { xs: '100%', sm: 'auto' } }}
        >
          Add Trigger
        </Button>
      </Box>
      
      {triggers.length === 0 ? (
        <Alert severity="info">No triggers found for this cron job.</Alert>
      ) : (
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
          <Table sx={{ minWidth: { xs: 700, sm: 'auto' } }}>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Name</TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Description</TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Cron Expression</TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Status</TableCell>
                <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Next Fire Time</TableCell>
                <TableCell align="center" sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {triggers.map((trigger) => (
                <TableRow key={trigger.id} hover>
                  <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    <Typography variant="body2" fontWeight="medium" sx={{ fontSize: 'inherit' }}>
                      {trigger.name}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    <Typography variant="body2" color="textSecondary" sx={{ fontSize: 'inherit' }}>
                      {trigger.description || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    <Typography variant="body2" fontFamily="monospace" sx={{ fontSize: 'inherit' }}>
                      {trigger.cronExpression}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    <Chip
                      label={trigger.enabled ? 'Enabled' : 'Disabled'}
                      color={trigger.enabled ? 'success' : 'default'}
                      size="small"
                      sx={{ 
                        fontSize: { xs: '0.65rem', sm: '0.75rem' },
                        height: { xs: 20, sm: 24 }
                      }}
                    />
                  </TableCell>
                  <TableCell sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    {(() => {
                      const dateTime = formatDateTime(trigger.nextFireTime);
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
                  <TableCell align="center" sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
                    <IconButton 
                      size="small"
                      onClick={() => onEditTrigger(trigger)}
                      sx={{ padding: { xs: '4px', sm: '8px' } }}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton 
                      size="small"
                      onClick={() => onDeleteTrigger(trigger.id!)}
                      sx={{ padding: { xs: '4px', sm: '8px' } }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default TriggerTable;
