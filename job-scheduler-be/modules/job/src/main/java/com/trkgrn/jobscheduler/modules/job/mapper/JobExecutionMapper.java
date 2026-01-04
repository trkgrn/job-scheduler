package com.trkgrn.jobscheduler.modules.job.mapper;

import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.platform.common.dto.JobExecutionDto;
import com.trkgrn.jobscheduler.platform.common.dto.LogEntryDto;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobExecutionMapper {

    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "jobDefinition.id", target = "jobDefinitionId")
    @Mapping(source = "startedAt", target = "startTime")
    @Mapping(source = "endedAt", target = "endTime")
    @Mapping(source = "attempt", target = "retryCount")
    @Mapping(source = "logs", target = "logs", qualifiedByName = "logEntriesToDto")
    @Mapping(source = "logLevel", target = "logLevel")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToOffsetDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "localDateTimeToOffsetDateTime")
    @Mapping(target = "duration", ignore = true)
    @Mapping(target = "result", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    JobExecutionDto toDto(JobExecutionModel entity);

    @Mapping(source = "status", target = "status", qualifiedByName = "stringToStatus")
    @Mapping(source = "jobDefinitionId", target = "jobDefinition", qualifiedByName = "mapJobDefinitionId")
    @Mapping(source = "startTime", target = "startedAt")
    @Mapping(source = "endTime", target = "endedAt")
    @Mapping(source = "retryCount", target = "attempt")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobExecutionModel toEntity(JobExecutionDto dto);


    @Named("statusToString")
    default String statusToString(JobExecutionModel.Status status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default JobExecutionModel.Status stringToStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return JobExecutionModel.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("mapJobDefinitionId")
    default com.trkgrn.jobscheduler.modules.job.model.CronJobModel mapJobDefinitionId(Long jobDefinitionId) {
        if (jobDefinitionId == null) {
            return null;
        }
        // Create a CronJobModel instance for the relationship
        com.trkgrn.jobscheduler.modules.job.model.CronJobModel cronJob = new com.trkgrn.jobscheduler.modules.job.model.CronJobModel();
        cronJob.setId(jobDefinitionId);
        return cronJob;
    }

    @Named("localDateTimeToOffsetDateTime")
    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }

    @Named("offsetDateTimeToLocalDateTime")
    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }

    @Named("logEntriesToDto")
    default List<LogEntryDto> logEntriesToDto(List<JobExecutionModel.LogEntry> logEntries) {
        if (logEntries == null) {
            return null;
        }
        return logEntries.stream()
                .map(logEntry -> new LogEntryDto(
                        logEntry.getTimestamp(),
                        logEntry.getLevel(),
                        logEntry.getMessage()
                ))
                .toList();
    }

    @AfterMapping
    default void calculateDuration(JobExecutionModel entity, @MappingTarget JobExecutionDto dto) {
        if (entity == null || entity.getStartedAt() == null) {
            dto.setDuration(null);
            return;
        }
        
        OffsetDateTime endTime = entity.getEndedAt();
        if (endTime == null) {
            // If job is still running, calculate duration from start to now
            endTime = OffsetDateTime.now();
        }
        
        Long duration = java.time.Duration.between(entity.getStartedAt(), endTime).toMillis();
        dto.setDuration(duration);
    }
}

