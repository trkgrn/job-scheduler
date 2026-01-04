package com.trkgrn.jobscheduler.modules.job.facade.impl;

import com.trkgrn.jobscheduler.modules.job.facade.TriggerFacade;
import com.trkgrn.jobscheduler.modules.job.mapper.TriggerMapper;
import com.trkgrn.jobscheduler.modules.job.mapper.helper.TriggerMappingHelper;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.service.TriggerService;
import com.trkgrn.jobscheduler.modules.job.service.TriggerSyncService;
import com.trkgrn.jobscheduler.platform.common.dto.TriggerDto;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DefaultTriggerFacade implements TriggerFacade {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTriggerFacade.class);

    private final TriggerService triggerService;
    private final TriggerSyncService triggerSyncService;
    private final TriggerMapper triggerMapper;
    private final TriggerMappingHelper triggerMappingHelper;

    public DefaultTriggerFacade(TriggerService triggerService, TriggerSyncService triggerSyncService,
                               TriggerMapper triggerMapper, TriggerMappingHelper triggerMappingHelper) {
        this.triggerService = triggerService;
        this.triggerSyncService = triggerSyncService;
        this.triggerMapper = triggerMapper;
        this.triggerMappingHelper = triggerMappingHelper;
    }

    @Override
    public DataResult<List<TriggerDto>> findAll() {
        List<TriggerModel> triggers = triggerService.findAll();
        List<TriggerDto> triggerDtos = triggers.stream()
                .map(triggerMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(triggerDtos, "Triggers fetched successfully");
    }

    @Override
    public DataResult<TriggerDto> findById(Long id) {
        TriggerModel triggerModel = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        TriggerDto triggerDto = triggerMapper.toDto(triggerModel);
        return new SuccessDataResult<>(triggerDto, "Trigger fetched successfully");
    }

    @Override
    public DataResult<List<TriggerDto>> findByCronJobId(Long cronJobId) {
        List<TriggerModel> triggers = triggerService.findByCronJobId(cronJobId);
        List<TriggerDto> triggerDtos = triggers.stream()
                .map(triggerMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(triggerDtos, "Triggers fetched successfully");
    }

    @Override
    public DataResult<TriggerDto> create(TriggerDto triggerDto) {
        TriggerModel triggerModel = triggerMappingHelper.toEntityWithCronJob(triggerDto);
        TriggerModel savedTrigger = triggerService.create(triggerModel);
        
        if (savedTrigger == null || savedTrigger.getId() == null) {
            throw new NotCreatedException("Failed to create trigger");
        }
        
        TriggerDto savedTriggerDto = triggerMapper.toDto(savedTrigger);
        return new SuccessDataResult<>(savedTriggerDto, "Trigger created successfully");
    }

    @Override
    public DataResult<TriggerDto> update(Long id, TriggerDto triggerDto) {
        TriggerModel existingTrigger = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        
        triggerMappingHelper.updateEntityWithCronJob(triggerDto, existingTrigger);
        existingTrigger.setId(id);
        
        TriggerModel updatedTrigger = triggerService.update(id, existingTrigger);
        
        if (updatedTrigger == null) {
            throw new NotUpdatedException("Failed to update trigger with id: " + id);
        }
        
        TriggerDto updatedTriggerDto = triggerMapper.toDto(updatedTrigger);
        return new SuccessDataResult<>(updatedTriggerDto, "Trigger updated successfully");
    }

    @Override
    public Result deleteById(Long id) {
        TriggerModel triggerModel = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        
        triggerService.deleteById(id);
        return new SuccessResult("Trigger deleted successfully");
    }

    @Override
    public DataResult<TriggerDto> pause(Long id) {
        TriggerModel triggerModel = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        
        TriggerModel pausedTrigger = triggerService.pause(id);
        TriggerDto pausedTriggerDto = triggerMapper.toDto(pausedTrigger);
        return new SuccessDataResult<>(pausedTriggerDto, "Trigger paused successfully");
    }

    @Override
    public DataResult<TriggerDto> resume(Long id) {
        TriggerModel triggerModel = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        
        TriggerModel resumedTrigger = triggerService.resume(id);
        TriggerDto resumedTriggerDto = triggerMapper.toDto(resumedTrigger);
        return new SuccessDataResult<>(resumedTriggerDto, "Trigger resumed successfully");
    }

    @Override
    public DataResult<Map<String, Object>> getNextFireTime(Long id) {
        TriggerModel triggerModel = triggerService.findById(id)
                .orElseThrow(() -> new NotFoundException("Trigger not found with id: " + id));
        
        OffsetDateTime nextFireTime = triggerService.getNextFireTime(id);
        Map<String, Object> response = Map.of(
            "triggerId", id,
            "nextFireTime", nextFireTime != null ? nextFireTime.toString() : "Not scheduled"
        );
        return new SuccessDataResult<>(response, "Next fire time fetched successfully");
    }

    @Override
    public DataResult<List<TriggerDto>> findReadyToFire() {
        List<TriggerModel> triggers = triggerService.findReadyToFire(OffsetDateTime.now());
        List<TriggerDto> triggerDtos = triggers.stream()
                .map(triggerMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(triggerDtos, "Ready to fire triggers fetched successfully");
    }

    @Override
    public DataResult<Map<String, Object>> syncTriggers() {
        triggerSyncService.syncAllTriggers();
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "Triggers synchronized successfully"
        );
        return new SuccessDataResult<>(response, "Triggers synchronized successfully");
    }
}

