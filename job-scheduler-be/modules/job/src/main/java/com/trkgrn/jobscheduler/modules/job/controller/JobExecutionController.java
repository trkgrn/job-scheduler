package com.trkgrn.jobscheduler.modules.job.controller;

import com.trkgrn.jobscheduler.modules.job.dto.ExecutionStatsDto;
import com.trkgrn.jobscheduler.modules.job.dto.PaginatedResponse;
import com.trkgrn.jobscheduler.modules.job.facade.JobExecutionFacade;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.platform.common.dto.JobExecutionDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/executions")
public class JobExecutionController {

    private final JobExecutionFacade jobExecutionFacade;

    public JobExecutionController(JobExecutionFacade jobExecutionFacade) {
        this.jobExecutionFacade = jobExecutionFacade;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result> getById(@PathVariable Long id) {
        DataResult<JobExecutionDto> result = jobExecutionFacade.findById(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping
    public ResponseEntity<Result> getAll() {
        DataResult<List<JobExecutionDto>> result = jobExecutionFacade.findAll();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/cron-job/{cronJobId}")
    public ResponseEntity<Result> getByCronJobId(@PathVariable Long cronJobId) {
        DataResult<List<JobExecutionDto>> result = jobExecutionFacade.findByCronJobId(cronJobId);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result> getByStatus(@PathVariable String status) {
        DataResult<List<JobExecutionDto>> result = jobExecutionFacade.findByStatus(status);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @GetMapping("/active")
    public ResponseEntity<Result> getActive() {
        DataResult<List<JobExecutionDto>> result = jobExecutionFacade.findActive();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Result> getStats(@RequestParam(required = false) Long cronJobId) {
        DataResult<ExecutionStatsDto> result = jobExecutionFacade.getStats(cronJobId);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Result> cancel(@PathVariable Long id) {
        DataResult<JobExecutionDto> result = jobExecutionFacade.cancel(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<Result> getLogs(@PathVariable Long id) {
        DataResult<List<JobExecutionModel.LogEntry>> result = jobExecutionFacade.getLogs(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Result> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        DataResult<PaginatedResponse<JobExecutionDto>> result = jobExecutionFacade.findAllPaginated(page, size);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/cron-job/{cronJobId}/paginated")
    public ResponseEntity<Result> getByCronJobIdPaginated(
            @PathVariable Long cronJobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        DataResult<PaginatedResponse<JobExecutionDto>> result = jobExecutionFacade.findByCronJobIdPaginated(cronJobId, page, size, status);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Result> getByStatusPaginated(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        DataResult<PaginatedResponse<JobExecutionDto>> result = jobExecutionFacade.findByStatusPaginated(status, page, size);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }
}
