export interface CronJobModel {
  id?: number;
  code?: string;
  name?: string;
  description?: string;
  enabled: boolean;
  jobBeanName?: string;
  status?: CronJobStatus;
  lastStartTime?: string;
  lastEndTime?: string;
  lastResult?: string;
  retryCount: number;
  maxRetries: number;
  parameters?: Record<string, any>;
  logLevel?: string;
  nodeId?: string;
  correlationId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum CronJobStatus {
  UNKNOWN = 'UNKNOWN',
  RUNNING = 'RUNNING',
  FINISHED = 'FINISHED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  PAUSED = 'PAUSED'
}

export interface TriggerModel {
  id?: number;
  name?: string;
  description?: string;
  cronJob?: CronJobModel;
  cronJobId?: number;
  cronJobCode?: string;
  cronExpression?: string;
  enabled: boolean;
  status?: TriggerStatus;
  startTime?: string;
  endTime?: string;
  nextFireTime?: string;
  lastFireTime?: string;
  fireCount?: number;
  priority: number;
  misfireInstruction?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum TriggerStatus {
  ACTIVE = 'ACTIVE',
  PAUSED = 'PAUSED',
  COMPLETE = 'COMPLETE',
  ERROR = 'ERROR',
  NONE = 'NONE'
}

export interface JobExecution {
  id?: number;
  jobDefinitionId?: number;
  status?: JobExecutionStatus;
  startTime?: string;
  endTime?: string;
  startedAt?: string;
  endedAt?: string;
  duration?: number;
  result?: string;
  errorMessage?: string;
  retryCount: number;
  attempt?: number;
  correlationId?: string;
  parameters?: Record<string, any>;
  logs?: LogEntry[];
  logLevel?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface LogEntry {
  timestamp: string;
  level: string;
  message: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export enum JobExecutionStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface JobExecutionLog {
  id?: number;
  executionId?: number;
  level?: string;
  message?: string;
  timestamp?: string;
  createdAt?: string;
  updatedAt?: string;
}



export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CronExpression {
  expression: string;
  description: string;
  example: string;
}

export interface JobStats {
  totalJobs: number;
  activeJobs: number;
  runningJobs: number;
  failedJobs: number;
  totalExecutions: number;
  successfulExecutions: number;
  failedExecutions: number;
  averageExecutionTime: number;
}

export interface StatusDistribution {
  status: string;
  count: number;
}

export interface ExecutionTrendData {
  date: string;
  successful: number;
  failed: number;
  total: number;
}

export interface TopJob {
  jobId: number;
  jobName: string;
  executionCount: number;
}

export interface StatsOverview {
  jobStats: JobStats;
  jobStatusDistribution: StatusDistribution[];
  triggerStatusDistribution: StatusDistribution[];
  executionTrend: ExecutionTrendData[];
  topJobsByExecution: TopJob[];
  cronJobs: MinimalCronJob[];
}

export interface MinimalCronJob {
  id: number;
  name: string;
}

// Job Metadata Types
export interface JobMetadata {
  beanName: string;
  displayName: string;
  description: string;
  category: string;
  parameters: JobParameter[];
  abortable?: boolean;
}

export interface JobParameter {
  name: string;
  type: ParameterType;
  displayName: string;
  description: string;
  required: boolean;
  defaultValue: string;
  validation: string;
  options: string;
}

export enum ParameterType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  BOOLEAN = 'BOOLEAN',
  JSON = 'JSON',
  ENUM = 'ENUM',
  DATE = 'DATE',
  TEXTAREA = 'TEXTAREA'
}

export enum LogLevel {
  TRACE = 'TRACE',
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
  OFF = 'OFF'
}

export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
}

export interface ValidationError {
  parameter: string;
  message: string;
}

// Result Types (matching backend)
export interface Result {
  success: boolean;
  message?: string;
}

export interface DataResult<T> extends Result {
  data: T;
}

export interface ErrorResult extends Result {
  errorDetail?: number;
}

export interface SuccessResult extends Result {
}

export interface SuccessDataResult<T> extends DataResult<T> {
}

export interface ErrorDataResult<T> extends DataResult<T> {
}
