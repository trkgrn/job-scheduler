import React from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Typography,
  Menu,
  MenuItem,
  Paper,
  Divider
} from '@mui/material';
import {
  MoreVert as MoreVertIcon,
  PlayArrow as PlayIcon,
  Edit as EditIcon,
  Delete as DeleteIcon
} from '@mui/icons-material';
import { CronJobModel } from '../../types';

interface CronJobListProps {
  cronJobs: CronJobModel[];
  selectedCronJob: CronJobModel | null;
  onSelectCronJob: (cronJob: CronJobModel) => void;
  onEditCronJob: (cronJob: CronJobModel) => void;
  onRunCronJob: (cronJob: CronJobModel) => void;
  onDeleteCronJob: (cronJob: CronJobModel) => void;
  anchorEl: HTMLElement | null;
  onMenuOpen: (event: React.MouseEvent<HTMLElement>, cronJob: CronJobModel) => void;
  onMenuClose: () => void;
  selectedMenuCronJob: CronJobModel | null;
}

const CronJobList: React.FC<CronJobListProps> = ({
  cronJobs,
  selectedCronJob,
  onSelectCronJob,
  onEditCronJob,
  onRunCronJob,
  onDeleteCronJob,
  anchorEl,
  onMenuOpen,
  onMenuClose,
  selectedMenuCronJob
}) => {
  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'RUNNING':
        return 'success';
      case 'PAUSED':
        return 'warning';
      case 'ERROR':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Paper sx={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      minHeight: { xs: '200px', md: 'auto' }
    }}>
      <Box sx={{ 
        p: { xs: 1.5, sm: 2 }, 
        borderBottom: '1px solid #eee' 
      }}>
        <Typography variant="h6" fontWeight="medium" sx={{ fontSize: { xs: '1rem', sm: '1.25rem' } }}>
          Cron Jobs
        </Typography>
        <Typography variant="body2" color="textSecondary" sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}>
          {cronJobs.length} job{cronJobs.length !== 1 ? 's' : ''}
        </Typography>
      </Box>
      
      <List sx={{ 
        flex: 1, 
        overflow: 'auto', 
        p: 0,
        '& .MuiListItem-root': {
          '&:last-child .MuiDivider-root': {
            display: 'none'
          }
        }
      }}>
        {cronJobs.map((cronJob) => (
          <React.Fragment key={cronJob.id}>
            <ListItem disablePadding>
              <ListItemButton
                selected={selectedCronJob?.id === cronJob.id}
                onClick={() => onSelectCronJob(cronJob)}
                sx={{
                  py: { xs: 1, sm: 1.5 },
                  px: { xs: 1, sm: 2 },
                  '&.Mui-selected': {
                    backgroundColor: 'primary.50',
                    '&:hover': {
                      backgroundColor: 'primary.100',
                    },
                  },
                }}
              >
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1} mb={0.5} sx={{ 
                      flexWrap: 'nowrap',
                      pr: { xs: 4, sm: 4 } // Right padding for menu button space
                    }}>
                      <Typography 
                        variant="subtitle2" 
                        fontWeight="medium"
                        sx={{ 
                          fontSize: { xs: '0.875rem', sm: '1rem' },
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          flex: 1,
                          minWidth: 0
                        }}
                      >
                        {cronJob.name}
                      </Typography>
                      <Chip
                        label={cronJob.status || 'UNKNOWN'}
                        color={getStatusColor(cronJob.status) as any}
                        size="small"
                        variant="outlined"
                        sx={{ 
                          fontSize: { xs: '0.65rem', sm: '0.75rem' },
                          height: { xs: 20, sm: 24 },
                          flexShrink: 0 // Prevent chip from shrinking
                        }}
                      />
                    </Box>
                  }
                  secondary={
                    <Typography 
                      variant="body2" 
                      color="textSecondary" 
                      sx={{ 
                        fontSize: { xs: '0.75rem', sm: '0.875rem' },
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        display: { xs: 'block', sm: 'block' }
                      }}
                    >
                      {cronJob.description || 'No description'}
                    </Typography>
                  }
                />
                <ListItemSecondaryAction>
                  <IconButton
                    size="small"
                    onClick={(e) => onMenuOpen(e, cronJob)}
                    sx={{ 
                      opacity: 0.7,
                      padding: { xs: '4px', sm: '8px' }
                    }}
                  >
                    <MoreVertIcon fontSize="small" />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItemButton>
            </ListItem>
            <Divider />
          </React.Fragment>
        ))}
      </List>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={onMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onEditCronJob(selectedMenuCronJob);
          }
          onMenuClose();
        }}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} />
          Edit
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onRunCronJob(selectedMenuCronJob);
          }
          onMenuClose();
        }}>
          <PlayIcon fontSize="small" sx={{ mr: 1 }} />
          Run Now
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onDeleteCronJob(selectedMenuCronJob);
          }
          onMenuClose();
        }} sx={{ color: 'error.main' }}>
          <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>
    </Paper>
  );
};

export default CronJobList;
