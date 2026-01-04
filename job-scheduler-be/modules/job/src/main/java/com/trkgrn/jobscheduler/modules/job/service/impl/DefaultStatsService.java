package com.trkgrn.jobscheduler.modules.job.service.impl;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import com.trkgrn.jobscheduler.modules.job.repository.TriggerRepository;
import com.trkgrn.jobscheduler.modules.job.service.StatsService;
import com.trkgrn.jobscheduler.platform.common.dto.ExecutionTrendDataDto;
import com.trkgrn.jobscheduler.platform.common.dto.MinimalCronJobDto;
import com.trkgrn.jobscheduler.platform.common.dto.StatusDistributionDto;
import com.trkgrn.jobscheduler.platform.common.dto.TopJobDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DefaultStatsService implements StatsService {

    private final CronJobRepository cronJobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final TriggerRepository triggerRepository;

    public DefaultStatsService(
            CronJobRepository cronJobRepository, 
            JobExecutionRepository jobExecutionRepository,
            TriggerRepository triggerRepository) {
        this.cronJobRepository = cronJobRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.triggerRepository = triggerRepository;
    }

    @Override
    public long getTotalJobs() {
        return cronJobRepository.count();
    }

    @Override
    public long getActiveJobs() {
        return cronJobRepository.countByEnabledTrue();
    }

    @Override
    public long getRunningJobs() {
        return cronJobRepository.countByStatus(CronJobStatus.RUNNING);
    }

    @Override
    public long getFailedJobs() {
        return cronJobRepository.countByStatus(CronJobStatus.FAILED);
    }

    @Override
    public long getTotalExecutions() {
        return jobExecutionRepository.count();
    }

    @Override
    public long getSuccessfulExecutions(Long cronJobId) {
        if (cronJobId != null) {
            return jobExecutionRepository.countByJobDefinitionIdAndStatus(cronJobId, JobExecutionModel.Status.SUCCESS);
        } else {
            return jobExecutionRepository.countByStatus(JobExecutionModel.Status.SUCCESS);
        }
    }

    @Override
    public long getFailedExecutions(Long cronJobId) {
        if (cronJobId != null) {
            return jobExecutionRepository.countByJobDefinitionIdAndStatus(cronJobId, JobExecutionModel.Status.FAILED);
        } else {
            return jobExecutionRepository.countByStatus(JobExecutionModel.Status.FAILED);
        }
    }

    @Override
    public double getAverageExecutionTime(Long cronJobId) {
        List<JobExecutionModel> executions;
        
        if (cronJobId != null) {
            executions = jobExecutionRepository.findByJobDefinitionId(cronJobId);
        } else {
            executions = jobExecutionRepository.findAll();
        }

        return executions.stream()
                .filter(execution -> execution.getStartedAt() != null && execution.getEndedAt() != null)
                .filter(execution -> execution.getStatus() == JobExecutionModel.Status.SUCCESS)
                .mapToLong(execution -> {
                    OffsetDateTime start = execution.getStartedAt();
                    OffsetDateTime end = execution.getEndedAt();
                    return end.toInstant().toEpochMilli() - start.toInstant().toEpochMilli();
                })
                .average()
                .orElse(0.0);
    }

    @Override
    public long getRunningExecutions() {
        return jobExecutionRepository.findAll().stream()
                .filter(execution -> execution.getStatus() == JobExecutionModel.Status.RUNNING)
                .count();
    }

    @Override
    public long getActiveExecutions() {
        return jobExecutionRepository.findByIsActiveTrue().size();
    }

    @Override
    public List<StatusDistributionDto> getJobStatusDistribution() {
        List<CronJobModel> allJobs = cronJobRepository.findAll();
        Map<String, Long> statusCounts = allJobs.stream()
                .collect(Collectors.groupingBy(
                        job -> job.getStatus() != null ? job.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));
        
        return statusCounts.entrySet().stream()
                .map(entry -> new StatusDistributionDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<StatusDistributionDto> getTriggerStatusDistribution() {
        List<TriggerModel> allTriggers = triggerRepository.findAll();
        Map<String, Long> statusCounts = allTriggers.stream()
                .collect(Collectors.groupingBy(
                        trigger -> trigger.getEnabled() ? "ACTIVE" : "PAUSED",
                        Collectors.counting()
                ));
        
        return statusCounts.entrySet().stream()
                .map(entry -> new StatusDistributionDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionTrendDataDto> getExecutionTrendData(int days) {
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(days);
        List<JobExecutionModel> executions = jobExecutionRepository.findByStartedAtAfter(startDate);

        Map<String, List<JobExecutionModel>> executionsByDate = executions.stream()
                .collect(Collectors.groupingBy(exec -> {
                    OffsetDateTime startedAt = exec.getStartedAt();
                    return startedAt != null ? startedAt.format(DateTimeFormatter.ISO_LOCAL_DATE) : "UNKNOWN";
                }));

        List<ExecutionTrendDataDto> trendData = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            OffsetDateTime date = OffsetDateTime.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            List<JobExecutionModel> dayExecutions = executionsByDate.getOrDefault(dateStr, new ArrayList<>());
            long successful = dayExecutions.stream()
                    .filter(exec -> exec.getStatus() == JobExecutionModel.Status.SUCCESS)
                    .count();
            long failed = dayExecutions.stream()
                    .filter(exec -> exec.getStatus() == JobExecutionModel.Status.FAILED)
                    .count();
            long total = dayExecutions.size();
            
            trendData.add(new ExecutionTrendDataDto(dateStr, successful, failed, total));
        }
        
        return trendData;
    }

    @Override
    public List<TopJobDto> getTopJobsByExecutionCount(int limit) {
        List<Object[]> executionCounts = jobExecutionRepository.countExecutionsByJobId();
        
        return executionCounts.stream()
                .limit(limit)
                .map(result -> {
                    Long jobId = ((Number) result[0]).longValue();
                    Long count = ((Number) result[1]).longValue();
                    CronJobModel job = cronJobRepository.findById(jobId).orElse(null);
                    String jobName = job != null ? job.getName() : "Job " + jobId;
                    return new TopJobDto(jobId, jobName, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MinimalCronJobDto> getAllCronJobsMinimal() {
        return cronJobRepository.findAll().stream()
                .map(job -> new MinimalCronJobDto(job.getId(), job.getName()))
                .collect(Collectors.toList());
    }
}

