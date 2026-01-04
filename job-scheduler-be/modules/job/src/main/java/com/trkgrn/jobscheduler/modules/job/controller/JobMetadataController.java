package com.trkgrn.jobscheduler.modules.job.controller;

import com.trkgrn.jobscheduler.modules.job.dto.JobMetadataDto;
import com.trkgrn.jobscheduler.modules.job.facade.JobMetadataFacade;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/job-metadata")
public class JobMetadataController {
    
    private final JobMetadataFacade jobMetadataFacade;
    
    public JobMetadataController(JobMetadataFacade jobMetadataFacade) {
        this.jobMetadataFacade = jobMetadataFacade;
    }
    
    @GetMapping
    public ResponseEntity<Result> getAllJobMetadata() {
        DataResult<List<JobMetadataDto>> result = jobMetadataFacade.getAllJobMetadata();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    @GetMapping("/{beanName}")
    public ResponseEntity<Result> getJobMetadata(@PathVariable String beanName) {
        DataResult<JobMetadataDto> result = jobMetadataFacade.getJobMetadata(beanName);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<Result> getJobsByCategory(@PathVariable String category) {
        DataResult<List<JobMetadataDto>> result = jobMetadataFacade.getJobsByCategory(category);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<Result> getCategories() {
        DataResult<Set<String>> result = jobMetadataFacade.getCategories();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    @GetMapping("/{beanName}/exists")
    public ResponseEntity<Result> jobExists(@PathVariable String beanName) {
        DataResult<Boolean> result = jobMetadataFacade.jobExists(beanName);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
    
    @PostMapping("/{beanName}/validate")
    public ResponseEntity<Result> validateParameters(
            @PathVariable String beanName, 
            @RequestBody Map<String, Object> request) {
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        DataResult<Map<String, Object>> result = jobMetadataFacade.validateParameters(beanName, parameters);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }
}
