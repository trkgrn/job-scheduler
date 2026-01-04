package com.trkgrn.jobscheduler.modules.job.facade.impl;

import com.trkgrn.jobscheduler.modules.job.facade.CronJobFacade;
import com.trkgrn.jobscheduler.modules.job.mapper.helper.CronJobMappingHelper;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.service.CronJobService;
import com.trkgrn.jobscheduler.platform.common.dto.CronJobDto;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotCreatedException;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotFoundException;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotUpdatedException;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import com.trkgrn.jobscheduler.platform.common.model.result.SuccessDataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.SuccessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultCronJobFacade implements CronJobFacade {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCronJobFacade.class);

    private final CronJobService cronJobService;
    private final CronJobMappingHelper cronJobMappingHelper;

    public DefaultCronJobFacade(CronJobService cronJobService, CronJobMappingHelper cronJobMappingHelper) {
        this.cronJobService = cronJobService;
        this.cronJobMappingHelper = cronJobMappingHelper;
    }

    @Override
    public DataResult<List<CronJobDto>> findAll() {
        List<CronJobModel> cronJobs = cronJobService.findAll();
        List<CronJobDto> cronJobDtos = cronJobs.stream()
                .map(cronJobMappingHelper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(cronJobDtos, "CronJobs fetched successfully");
    }

    @Override
    public DataResult<CronJobDto> findById(Long id) {
        CronJobModel cronJobModel = cronJobService.findById(id)
                .orElseThrow(() -> new NotFoundException("CronJob not found with id: " + id));
        CronJobDto cronJobDto = cronJobMappingHelper.toDto(cronJobModel);
        return new SuccessDataResult<>(cronJobDto, "CronJob fetched successfully");
    }

    @Override
    public DataResult<CronJobDto> findByCode(String code) {
        CronJobModel cronJobModel = cronJobService.findByCode(code)
                .orElseThrow(() -> new NotFoundException("CronJob not found with code: " + code));
        CronJobDto cronJobDto = cronJobMappingHelper.toDto(cronJobModel);
        return new SuccessDataResult<>(cronJobDto, "CronJob fetched successfully");
    }

    @Override
    public DataResult<CronJobDto> create(CronJobDto cronJobDto) {
        CronJobModel cronJobModel = cronJobMappingHelper.toEntity(cronJobDto);
        CronJobModel savedCronJob = cronJobService.save(cronJobModel);
        
        if (savedCronJob == null || savedCronJob.getId() == null) {
            throw new NotCreatedException("Failed to create CronJob");
        }
        
        CronJobDto savedCronJobDto = cronJobMappingHelper.toDto(savedCronJob);
        return new SuccessDataResult<>(savedCronJobDto, "CronJob created successfully");
    }

    @Override
    public DataResult<CronJobDto> update(Long id, CronJobDto cronJobDto) {
        LOG.info("Updating CronJob ID: {} with parameters: {}", id, cronJobDto.getParameters());
        
        CronJobModel existingCronJob = cronJobService.findById(id)
                .orElseThrow(() -> new NotFoundException("CronJob not found with id: " + id));
        
        LOG.info("Existing CronJob parameters before update: {}", existingCronJob.getParameters());
        
        cronJobMappingHelper.updateEntity(cronJobDto, existingCronJob);
        existingCronJob.setId(id);
        
        LOG.info("Updated CronJob parameters after mapping: {}", existingCronJob.getParameters());
        
        CronJobModel updatedCronJob = cronJobService.save(existingCronJob);
        
        if (updatedCronJob == null) {
            throw new NotUpdatedException("Failed to update CronJob with id: " + id);
        }
        
        CronJobDto updatedCronJobDto = cronJobMappingHelper.toDto(updatedCronJob);
        
        LOG.info("Final CronJob parameters after save: {}", updatedCronJobDto.getParameters());
        
        return new SuccessDataResult<>(updatedCronJobDto, "CronJob updated successfully");
    }

    @Override
    public Result deleteById(Long id) {
        CronJobModel cronJobModel = cronJobService.findById(id)
                .orElseThrow(() -> new NotFoundException("CronJob not found with id: " + id));
        
        cronJobService.deleteById(id);
        return new SuccessResult("CronJob deleted successfully");
    }

    @Override
    public DataResult<CronJobDto> runNow(Long id) {
        CronJobModel cronJobModel = cronJobService.findById(id)
                .orElseThrow(() -> new NotFoundException("CronJob not found with id: " + id));
        
        CronJobModel updatedCronJob = cronJobService.runNow(id);
        CronJobDto updatedCronJobDto = cronJobMappingHelper.toDto(updatedCronJob);
        return new SuccessDataResult<>(updatedCronJobDto, "CronJob executed successfully");
    }

    @Override
    public DataResult<List<String>> getAvailableJobs() {
        List<String> availableJobs = cronJobService.getAvailableJobs();
        return new SuccessDataResult<>(availableJobs, "Available jobs fetched successfully");
    }
}

