package com.trkgrn.jobscheduler.modules.job.facade;

import com.trkgrn.jobscheduler.modules.job.dto.JobMetadataDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JobMetadataFacade {
    DataResult<List<JobMetadataDto>> getAllJobMetadata();
    DataResult<JobMetadataDto> getJobMetadata(String beanName);
    DataResult<List<JobMetadataDto>> getJobsByCategory(String category);
    DataResult<Set<String>> getCategories();
    DataResult<Boolean> jobExists(String beanName);
    DataResult<Map<String, Object>> validateParameters(String beanName, Map<String, Object> parameters);
}

