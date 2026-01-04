package com.trkgrn.jobscheduler.modules.job.controller;

import com.trkgrn.jobscheduler.modules.job.facade.StatsFacade;
import com.trkgrn.jobscheduler.platform.common.dto.*;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/stats")
public class StatsController {

    private final StatsFacade statsFacade;

    public StatsController(StatsFacade statsFacade) {
        this.statsFacade = statsFacade;
    }

    @GetMapping("/jobs")
    public ResponseEntity<Result> getJobStats() {
        DataResult<JobStatsDto> result = statsFacade.getJobStats();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/executions")
    public ResponseEntity<Result> getExecutionStats(@RequestParam(required = false) Long cronJobId) {
        DataResult<JobStatsDto> result = statsFacade.getExecutionStats(cronJobId);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<Result> getOverviewStats(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int topJobsLimit) {
        DataResult<StatsOverviewDto> result = statsFacade.getOverviewStats(days, topJobsLimit);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/job-status-distribution")
    public ResponseEntity<Result> getJobStatusDistribution() {
        DataResult<List<StatusDistributionDto>> result = statsFacade.getJobStatusDistribution();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/trigger-status-distribution")
    public ResponseEntity<Result> getTriggerStatusDistribution() {
        DataResult<List<StatusDistributionDto>> result = statsFacade.getTriggerStatusDistribution();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/execution-trend")
    public ResponseEntity<Result> getExecutionTrend(@RequestParam(defaultValue = "7") int days) {
        DataResult<List<ExecutionTrendDataDto>> result = statsFacade.getExecutionTrend(days);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/top-jobs")
    public ResponseEntity<Result> getTopJobsByExecution(@RequestParam(defaultValue = "10") int limit) {
        DataResult<List<TopJobDto>> result = statsFacade.getTopJobsByExecution(limit);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
}
