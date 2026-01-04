package com.trkgrn.jobscheduler.modules.job.facade;

import com.trkgrn.jobscheduler.platform.common.dto.CronJobDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;

import java.util.List;

public interface CronJobFacade {
    DataResult<List<CronJobDto>> findAll();
    DataResult<CronJobDto> findById(Long id);
    DataResult<CronJobDto> findByCode(String code);
    DataResult<CronJobDto> create(CronJobDto cronJobDto);
    DataResult<CronJobDto> update(Long id, CronJobDto cronJobDto);
    Result deleteById(Long id);
    DataResult<CronJobDto> runNow(Long id);
    DataResult<List<String>> getAvailableJobs();
}

