package com.trkgrn.jobscheduler.modules.job.mapper.helper;

import com.trkgrn.jobscheduler.modules.job.mapper.TriggerMapper;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.platform.common.dto.TriggerDto;
import org.springframework.stereotype.Component;

@Component
public class TriggerMappingHelper {

    private final TriggerMapper triggerMapper;
    private final CronJobRepository cronJobRepository;

    public TriggerMappingHelper(TriggerMapper triggerMapper, CronJobRepository cronJobRepository) {
        this.triggerMapper = triggerMapper;
        this.cronJobRepository = cronJobRepository;
    }

    public TriggerModel toEntityWithCronJob(TriggerDto dto) {
        TriggerModel entity = triggerMapper.toEntity(dto);

        // Set cronJob relationship if cronJobId is provided
        if (dto.getCronJobId() != null) {
            CronJobModel cronJob = cronJobRepository.findById(dto.getCronJobId())
                    .orElseThrow(() -> new RuntimeException("CronJob not found with id: " + dto.getCronJobId()));
            entity.setCronJob(cronJob);
        }

        return entity;
    }

    public TriggerModel updateEntityWithCronJob(TriggerDto dto, TriggerModel existingEntity) {
        triggerMapper.updateEntity(dto, existingEntity);

        // Update cronJob relationship if cronJobId is provided
        if (dto.getCronJobId() != null) {
            CronJobModel cronJob = cronJobRepository.findById(dto.getCronJobId())
                    .orElseThrow(() -> new RuntimeException("CronJob not found with id: " + dto.getCronJobId()));
            existingEntity.setCronJob(cronJob);
        }

        return existingEntity;
    }
}

