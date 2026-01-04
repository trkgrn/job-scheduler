package com.trkgrn.jobscheduler.modules.job.mapper.helper;

import com.trkgrn.jobscheduler.modules.job.mapper.CronJobMapper;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.platform.common.dto.CronJobDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class CronJobMappingHelper {

    private final CronJobMapper cronJobMapper;

    public CronJobMappingHelper(CronJobMapper cronJobMapper) {
        this.cronJobMapper = cronJobMapper;
    }

    public CronJobDto toDto(CronJobModel entity) {
        return cronJobMapper.toDto(entity);
    }

    public CronJobModel toEntity(CronJobDto dto) {
        // Always create CronJobModel - parameters are stored in parameters JSON
        CronJobModel entity = new CronJobModel();
        populateBaseFields(dto, entity);
        return entity;
    }

    private void populateBaseFields(CronJobDto dto, CronJobModel entity) {
        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setJobBeanName(dto.getJobBeanName());
        entity.setEnabled(dto.getEnabled());

        if (dto.getStatus() != null) {
            try {
                entity.setStatus(CronJobStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                entity.setStatus(CronJobStatus.UNKNOWN);
            }
        }

        entity.setLastStartTime(dto.getLastStartTime());
        entity.setLastEndTime(dto.getLastEndTime());
        entity.setLastResult(dto.getLastResult());
        entity.setRetryCount(dto.getRetryCount());
        entity.setMaxRetryCount(dto.getMaxRetries());
        entity.setParameters(dto.getParameters());

        if (dto.getLogLevel() != null) {
            try {
                entity.setLogLevel(CronJobModel.LogLevel.valueOf(dto.getLogLevel()));
            } catch (IllegalArgumentException e) {
                entity.setLogLevel(CronJobModel.LogLevel.INFO);
            }
        }
    }

    public void updateEntity(CronJobDto dto, CronJobModel entity) {
        if (dto.getCode() != null) entity.setCode(dto.getCode());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getJobBeanName() != null) entity.setJobBeanName(dto.getJobBeanName());
        entity.setEnabled(dto.getEnabled());

        if (dto.getStatus() != null) {
            try {
                entity.setStatus(CronJobStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                entity.setStatus(CronJobStatus.UNKNOWN);
            }
        }

        if (dto.getLastStartTime() != null) entity.setLastStartTime(dto.getLastStartTime());
        if (dto.getLastEndTime() != null) entity.setLastEndTime(dto.getLastEndTime());
        if (dto.getLastResult() != null) entity.setLastResult(dto.getLastResult());
        entity.setRetryCount(dto.getRetryCount());
        entity.setMaxRetryCount(dto.getMaxRetries());
        // Always update parameters - if null, set empty map
        entity.setParameters(dto.getParameters() != null ? dto.getParameters() : new HashMap<>());

        if (dto.getLogLevel() != null) {
            try {
                entity.setLogLevel(CronJobModel.LogLevel.valueOf(dto.getLogLevel()));
            } catch (IllegalArgumentException e) {
                // Keep existing log level if invalid
            }
        }
    }

}

