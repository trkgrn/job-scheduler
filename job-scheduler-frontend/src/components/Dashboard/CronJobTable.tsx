import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  TablePagination,
  TextField,
  InputAdornment,
  Tooltip,
  TableSortLabel,
  Checkbox,
  Switch,
  FormControlLabel,
} from '@mui/material';
import {
  MoreVert as MoreVertIcon,
  PlayArrow as PlayIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';
import { CronJobModel } from '../../types';

interface CronJobTableProps {
  cronJobs: CronJobModel[];
  selectedCronJobs: CronJobModel[];
  onSelectCronJob: (cronJob: CronJobModel) => void;
  onSelectMultipleCronJobs: (cronJobs: CronJobModel[]) => void;
  onEditCronJob: (cronJob: CronJobModel) => void;
  onRunCronJob: (cronJob: CronJobModel) => void;
  onDeleteCronJob: (cronJob: CronJobModel) => void;
  onDeleteMultipleCronJobs: (cronJobs: CronJobModel[]) => void;
  loading?: boolean;
}

type Order = 'asc' | 'desc';
type OrderBy = 'name' | 'code' | 'group' | 'status' | 'enabled';

const CronJobTable: React.FC<CronJobTableProps> = ({
  cronJobs,
  selectedCronJobs,
  onSelectCronJob,
  onSelectMultipleCronJobs,
  onEditCronJob,
  onRunCronJob,
  onDeleteCronJob,
  onDeleteMultipleCronJobs,
  loading = false,
}) => {
  const [order, setOrder] = useState<Order>('asc');
  const [orderBy, setOrderBy] = useState<OrderBy>('name');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedMenuCronJob, setSelectedMenuCronJob] = useState<CronJobModel | null>(null);

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'RUNNING':
        return 'success';
      case 'PAUSED':
        return 'warning';
      case 'FAILED':
        return 'error';
      case 'FINISHED':
        return 'info';
      case 'CANCELLED':
        return 'default';
      case 'UNKNOWN':
        return 'default';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status?: string) => {
    switch (status) {
      case 'RUNNING':
        return 'Running';
      case 'PAUSED':
        return 'Paused';
      case 'FAILED':
        return 'Failed';
      case 'FINISHED':
        return 'Finished';
      case 'CANCELLED':
        return 'Cancelled';
      case 'UNKNOWN':
        return 'Unknown';
      default:
        return 'Unknown';
    }
  };

  const handleRequestSort = (property: OrderBy) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };

  const handleSelectAllClick = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.checked) {
      onSelectMultipleCronJobs(filteredCronJobs);
    } else {
      onSelectMultipleCronJobs([]);
    }
  };

  const handleSelectClick = (cronJob: CronJobModel) => {
    const selectedIndex = selectedCronJobs.findIndex(job => job.id === cronJob.id);
    let newSelected: CronJobModel[] = [];

    if (selectedIndex === -1) {
      newSelected = [...selectedCronJobs, cronJob];
    } else {
      newSelected = selectedCronJobs.filter(job => job.id !== cronJob.id);
    }

    onSelectMultipleCronJobs(newSelected);
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, cronJob: CronJobModel) => {
    setAnchorEl(event.currentTarget);
    setSelectedMenuCronJob(cronJob);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedMenuCronJob(null);
  };

  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const filteredCronJobs = cronJobs.filter(cronJob =>
    cronJob.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cronJob.code?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cronJob.jobBeanName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cronJob.description?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const sortedCronJobs = filteredCronJobs.sort((a, b) => {
    let aValue: any = '';
    let bValue: any = '';

    switch (orderBy) {
      case 'name':
        aValue = a.name || a.code || '';
        bValue = b.name || b.code || '';
        break;
      case 'group':
        aValue = a.jobBeanName || '';
        bValue = b.jobBeanName || '';
        break;
      case 'status':
        aValue = a.status || '';
        bValue = b.status || '';
        break;
      case 'code':
        aValue = a.code || '';
        bValue = b.code || '';
        break;
      case 'enabled':
        aValue = a.enabled ? 1 : 0;
        bValue = b.enabled ? 1 : 0;
        break;
    }

    if (order === 'asc') {
      return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
    } else {
      return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
    }
  });

  const paginatedCronJobs = sortedCronJobs.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  );

  const isSelected = (cronJob: CronJobModel) => 
    selectedCronJobs.findIndex(job => job.id === cronJob.id) !== -1;

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleString();
  };

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      {/* Header */}
      <Box sx={{ 
        p: 2, 
        borderBottom: '1px solid #e0e0e0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        backgroundColor: '#f5f5f5'
      }}>
        <Typography variant="h6" fontWeight="medium">
          Cron Jobs ({cronJobs.length})
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <TextField
            size="small"
            placeholder="Search cron jobs..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: 200 }}
          />
          <IconButton size="small">
            <FilterIcon />
          </IconButton>
        </Box>
      </Box>

      {/* Table */}
      <TableContainer sx={{ maxHeight: 600 }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow sx={{ backgroundColor: '#fafafa' }}>
              <TableCell padding="checkbox">
                <Checkbox
                  indeterminate={selectedCronJobs.length > 0 && selectedCronJobs.length < filteredCronJobs.length}
                  checked={filteredCronJobs.length > 0 && selectedCronJobs.length === filteredCronJobs.length}
                  onChange={handleSelectAllClick}
                />
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'name'}
                  direction={orderBy === 'name' ? order : 'asc'}
                  onClick={() => handleRequestSort('name')}
                >
                  Job Name
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'code'}
                  direction={orderBy === 'code' ? order : 'asc'}
                  onClick={() => handleRequestSort('code')}
                >
                  Code
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'group'}
                  direction={orderBy === 'group' ? order : 'asc'}
                  onClick={() => handleRequestSort('group')}
                >
                  Group
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'status'}
                  direction={orderBy === 'status' ? order : 'asc'}
                  onClick={() => handleRequestSort('status')}
                >
                  Status
                </TableSortLabel>
              </TableCell>
              <TableCell>
                <TableSortLabel
                  active={orderBy === 'enabled'}
                  direction={orderBy === 'enabled' ? order : 'asc'}
                  onClick={() => handleRequestSort('enabled')}
                >
                  Enabled
                </TableSortLabel>
              </TableCell>
              <TableCell>Description</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {paginatedCronJobs.map((cronJob) => {
              const isItemSelected = isSelected(cronJob);
              return (
                <TableRow
                  key={cronJob.id}
                  hover
                  selected={isItemSelected}
                  onClick={() => onSelectCronJob(cronJob)}
                  sx={{ 
                    cursor: 'pointer',
                    '&:hover': {
                      backgroundColor: '#f5f5f5'
                    }
                  }}
                >
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={isItemSelected}
                      onChange={() => handleSelectClick(cronJob)}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {cronJob.name || 'Unnamed'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="textSecondary">
                      {cronJob.code || '-'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="textSecondary">
                      {cronJob.jobBeanName || 'DEFAULT'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={getStatusLabel(cronJob.status)}
                      color={getStatusColor(cronJob.status) as any}
                      size="small"
                      variant="filled"
                    />
                  </TableCell>
                  <TableCell>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={cronJob.enabled}
                          size="small"
                          disabled
                          sx={{
                            '& .MuiSwitch-switchBase.Mui-disabled': {
                              color: cronJob.enabled ? 'success.main' : 'action.disabled',
                            },
                            '& .MuiSwitch-switchBase.Mui-disabled + .MuiSwitch-track': {
                              backgroundColor: cronJob.enabled ? 'success.main' : 'action.disabledBackground',
                              opacity: 1,
                            },
                          }}
                        />
                      }
                      label={cronJob.enabled ? 'Enabled' : 'Disabled'}
                      labelPlacement="end"
                      sx={{ 
                        margin: 0,
                        '& .MuiFormControlLabel-label': {
                          fontSize: '0.875rem',
                          color: cronJob.enabled ? 'success.main' : 'text.secondary',
                          fontWeight: cronJob.enabled ? 500 : 400,
                        }
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography 
                      variant="body2" 
                      color="textSecondary"
                      sx={{ 
                        maxWidth: 200,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}
                    >
                      {cronJob.description || 'No description'}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleMenuOpen(e, cronJob);
                      }}
                    >
                      <MoreVertIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Pagination */}
      <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={filteredCronJobs.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
      />

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
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
            onSelectCronJob(selectedMenuCronJob);
          }
          handleMenuClose();
        }}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} />
          View Details
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onRunCronJob(selectedMenuCronJob);
          }
          handleMenuClose();
        }}>
          <PlayIcon fontSize="small" sx={{ mr: 1 }} />
          Run Now
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onEditCronJob(selectedMenuCronJob);
          }
          handleMenuClose();
        }}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} />
          Edit
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedMenuCronJob) {
            onDeleteCronJob(selectedMenuCronJob);
          }
          handleMenuClose();
        }} sx={{ color: 'error.main' }}>
          <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>
    </Paper>
  );
};

export default CronJobTable;
