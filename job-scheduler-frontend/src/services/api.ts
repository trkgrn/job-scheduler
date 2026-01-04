import axios from 'axios';
import { 
  CronJobModel, 
  TriggerModel, 
  JobExecution, 
  JobStats, 
  JobMetadata, 
  ValidationResult, 
  LogEntry, 
  PaginatedResponse,
  DataResult,
  Result,
  StatsOverview,
  StatusDistribution,
  ExecutionTrendData,
  TopJob,
  MinimalCronJob
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

const extractData = <T>(result: DataResult<T>): T => {
  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }
  return result.data;
};

export const cronJobApi = {
  getAll: () => api.get<DataResult<CronJobModel[]>>('/v1/cron-jobs').then(res => res.data),
  getById: (id: number) => api.get<DataResult<CronJobModel>>(`/v1/cron-jobs/${id}`).then(res => res.data),
  getByCode: (code: string) => api.get<DataResult<CronJobModel>>(`/v1/cron-jobs/code/${code}`).then(res => res.data),
  create: (cronJob: Partial<CronJobModel>) => api.post<DataResult<CronJobModel>>('/v1/cron-jobs', cronJob).then(res => res.data),
  update: (id: number, cronJob: Partial<CronJobModel>) => api.put<DataResult<CronJobModel>>(`/v1/cron-jobs/${id}`, cronJob).then(res => res.data),
  delete: (id: number) => api.delete<Result>(`/v1/cron-jobs/${id}`).then(res => res.data),
  runNow: (id: number) => api.post<DataResult<CronJobModel>>(`/v1/cron-jobs/${id}/run-now`).then(res => res.data),
  getAvailableJobs: () => api.get<DataResult<string[]>>('/v1/cron-jobs/available-jobs').then(res => res.data),
};

export const triggerApi = {
  getAll: () => api.get<DataResult<TriggerModel[]>>('/v1/triggers').then(res => res.data),
  getById: (id: number) => api.get<DataResult<TriggerModel>>(`/v1/triggers/${id}`).then(res => res.data),
  getByCronJobId: (cronJobId: number) => api.get<DataResult<TriggerModel[]>>(`/v1/triggers/cron-job/${cronJobId}`).then(res => res.data),
  create: (trigger: Partial<TriggerModel>) => api.post<DataResult<TriggerModel>>('/v1/triggers', trigger).then(res => res.data),
  update: (id: number, trigger: Partial<TriggerModel>) => api.put<DataResult<TriggerModel>>(`/v1/triggers/${id}`, trigger).then(res => res.data),
  delete: (id: number) => api.delete<Result>(`/v1/triggers/${id}`).then(res => res.data),
  pause: (id: number) => api.post<DataResult<TriggerModel>>(`/v1/triggers/${id}/pause`).then(res => res.data),
  resume: (id: number) => api.post<DataResult<TriggerModel>>(`/v1/triggers/${id}/resume`).then(res => res.data),
  getNextFireTime: (id: number) => api.get<DataResult<{triggerId: number, nextFireTime: string}>>(`/v1/triggers/${id}/next-fire-time`).then(res => res.data),
  getReadyToFire: () => api.get<DataResult<TriggerModel[]>>('/v1/triggers/ready-to-fire').then(res => res.data),
  syncTriggers: () => api.post<DataResult<{success: boolean, message: string}>>('/v1/triggers/sync').then(res => res.data),
};

export const jobExecutionApi = {
  getAll: () => api.get<DataResult<JobExecution[]>>('/v1/executions').then(res => res.data),
  getByCronJobId: (cronJobId: number) => api.get<DataResult<JobExecution[]>>(`/v1/executions/cron-job/${cronJobId}`).then(res => res.data),
  getById: (id: number) => api.get<DataResult<JobExecution>>(`/v1/executions/${id}`).then(res => res.data),
  getByStatus: (status: string) => api.get<DataResult<JobExecution[]>>(`/v1/executions/status/${status}`).then(res => res.data),
  getActive: () => api.get<DataResult<JobExecution[]>>('/v1/executions/active').then(res => res.data),
  cancel: (id: number) => api.post<DataResult<JobExecution>>(`/v1/executions/${id}/cancel`).then(res => res.data),
  getLogs: (id: number) => api.get<DataResult<LogEntry[]>>(`/v1/executions/${id}/logs`).then(res => res.data),
  getStats: (cronJobId?: number) => api.get<DataResult<{successCount: number, failedCount: number, totalCount: number}>>(`/v1/executions/stats${cronJobId ? `?cronJobId=${cronJobId}` : ''}`).then(res => res.data),
  
  getAllPaginated: (page: number = 0, size: number = 10) => api.get<DataResult<PaginatedResponse<JobExecution>>>(`/v1/executions/paginated?page=${page}&size=${size}`).then(res => res.data),
  getByCronJobIdPaginated: (cronJobId: number, page: number = 0, size: number = 10, status?: string) => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
    if (status) params.append('status', status);
    return api.get<DataResult<PaginatedResponse<JobExecution>>>(`/v1/executions/cron-job/${cronJobId}/paginated?${params.toString()}`).then(res => res.data);
  },
  getByStatusPaginated: (status: string, page: number = 0, size: number = 10) => api.get<DataResult<PaginatedResponse<JobExecution>>>(`/v1/executions/status/${status}/paginated?page=${page}&size=${size}`).then(res => res.data),
};

export const jobMetadataApi = {
  getAll: () => api.get<DataResult<JobMetadata[]>>('/v1/job-metadata').then(res => res.data),
  getByBeanName: (beanName: string) => api.get<DataResult<JobMetadata>>(`/v1/job-metadata/${beanName}`).then(res => res.data),
  validateParameters: (beanName: string, parameters: Record<string, any>) => 
    api.post<DataResult<ValidationResult>>(`/v1/job-metadata/${beanName}/validate`, { parameters }).then(res => res.data),
};

export const statsApi = {
  getJobStats: () => api.get<DataResult<JobStats>>('/v1/stats/jobs').then(res => res.data),
  getExecutionStats: (cronJobId?: number) => api.get<DataResult<JobStats>>(`/v1/stats/executions${cronJobId ? `?cronJobId=${cronJobId}` : ''}`).then(res => res.data),
  getOverviewStats: (days: number = 7, topJobsLimit: number = 10) => 
    api.get<DataResult<StatsOverview>>(`/v1/stats/overview?days=${days}&topJobsLimit=${topJobsLimit}`).then(res => res.data),
  getJobStatusDistribution: () => api.get<DataResult<StatusDistribution[]>>(`/v1/stats/job-status-distribution`).then(res => res.data),
  getTriggerStatusDistribution: () => api.get<DataResult<StatusDistribution[]>>(`/v1/stats/trigger-status-distribution`).then(res => res.data),
  getExecutionTrend: (days: number = 7) => api.get<DataResult<ExecutionTrendData[]>>(`/v1/stats/execution-trend?days=${days}`).then(res => res.data),
  getTopJobsByExecution: (limit: number = 10) => api.get<DataResult<TopJob[]>>(`/v1/stats/top-jobs?limit=${limit}`).then(res => res.data),
};

export default api;
