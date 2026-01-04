package com.trkgrn.jobscheduler.modules.job.controller;

import com.trkgrn.jobscheduler.modules.job.facade.CronJobFacade;
import com.trkgrn.jobscheduler.platform.common.dto.CronJobDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/cron-jobs")
public class CronJobController {

    private final CronJobFacade cronJobFacade;

    public CronJobController(CronJobFacade cronJobFacade) {
        this.cronJobFacade = cronJobFacade;
    }

    @GetMapping
    public ResponseEntity<Result> list() {
        DataResult<List<CronJobDto>> result = cronJobFacade.findAll();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result> get(@PathVariable Long id) {
        DataResult<CronJobDto> result = cronJobFacade.findById(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Result> getByCode(@PathVariable String code) {
        DataResult<CronJobDto> result = cronJobFacade.findByCode(code);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping
    public ResponseEntity<Result> create(@RequestBody CronJobDto cronJobDto) {
        DataResult<CronJobDto> result = cronJobFacade.create(cronJobDto);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result> update(@PathVariable Long id, @RequestBody CronJobDto cronJobDto) {
        DataResult<CronJobDto> result = cronJobFacade.update(id, cronJobDto);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Long id) {
        Result result = cronJobFacade.deleteById(id);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.NO_CONTENT : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping("/{id}/run-now")
    public ResponseEntity<Result> runNow(@PathVariable Long id) {
        DataResult<CronJobDto> result = cronJobFacade.runNow(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @GetMapping("/available-jobs")
    public ResponseEntity<Result> getAvailableJobs() {
        DataResult<List<String>> result = cronJobFacade.getAvailableJobs();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }
}

