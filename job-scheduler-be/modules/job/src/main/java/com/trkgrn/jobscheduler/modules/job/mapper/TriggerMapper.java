package com.trkgrn.jobscheduler.modules.job.mapper;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.scheduler.QuartzJobScheduler;
import com.trkgrn.jobscheduler.platform.common.dto.CronJobDto;
import com.trkgrn.jobscheduler.platform.common.dto.TriggerDto;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class TriggerMapper {

    @Autowired
    protected QuartzJobScheduler quartzJobScheduler;

    @Mapping(source = "cronJob.id", target = "cronJobId")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToOffsetDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "localDateTimeToOffsetDateTime")
    @Mapping(source = "cronJob", target = "cronJob", qualifiedByName = "cronJobToDto")
    @Mapping(target = "nextFireTime", ignore = true)
    public abstract TriggerDto toDto(TriggerModel entity);

    @Mapping(target = "cronJob", ignore = true)
    public abstract TriggerModel toEntity(TriggerDto dto);

    @Mapping(target = "cronJob", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract TriggerModel updateEntity(TriggerDto dto, @MappingTarget TriggerModel entity);

    @Named("localDateTimeToOffsetDateTime")
    protected OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }

    @Named("cronJobToDto")
    protected CronJobDto cronJobToDto(CronJobModel cronJob) {
        if (cronJob == null) {
            return null;
        }
        CronJobDto dto = new CronJobDto();
        dto.setId(cronJob.getId());
        dto.setCode(cronJob.getCode());
        dto.setName(cronJob.getName());
        dto.setDescription(cronJob.getDescription());
        dto.setJobBeanName(cronJob.getJobBeanName());
        dto.setEnabled(cronJob.getEnabled());
        dto.setCreatedAt(cronJob.getCreatedAt() != null ? cronJob.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        dto.setUpdatedAt(cronJob.getUpdatedAt() != null ? cronJob.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        return dto;
    }

    @AfterMapping
    protected void calculateNextFireTime(TriggerModel entity, @MappingTarget TriggerDto dto) {
        if (entity != null && entity.getEnabled()) {
            try {
                OffsetDateTime nextFireTime = quartzJobScheduler.getNextFireTime(entity);
                dto.setNextFireTime(nextFireTime);
            } catch (Exception e) {
                // Log error but don't fail the mapping
                System.err.println("Failed to calculate next fire time for trigger " + entity.getId() + ": " + e.getMessage());
                dto.setNextFireTime(null);
            }
        } else {
            dto.setNextFireTime(null);
        }
    }
}

