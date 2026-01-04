package com.trkgrn.jobscheduler.modules.job.facade;

import com.trkgrn.jobscheduler.platform.common.dto.TriggerDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;

import java.util.List;
import java.util.Map;

public interface TriggerFacade {
    DataResult<List<TriggerDto>> findAll();
    DataResult<TriggerDto> findById(Long id);
    DataResult<List<TriggerDto>> findByCronJobId(Long cronJobId);
    DataResult<TriggerDto> create(TriggerDto triggerDto);
    DataResult<TriggerDto> update(Long id, TriggerDto triggerDto);
    Result deleteById(Long id);
    DataResult<TriggerDto> pause(Long id);
    DataResult<TriggerDto> resume(Long id);
    DataResult<Map<String, Object>> getNextFireTime(Long id);
    DataResult<List<TriggerDto>> findReadyToFire();
    DataResult<Map<String, Object>> syncTriggers();
}

