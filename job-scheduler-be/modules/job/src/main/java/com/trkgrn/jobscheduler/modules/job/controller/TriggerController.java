package com.trkgrn.jobscheduler.modules.job.controller;

import com.trkgrn.jobscheduler.modules.job.facade.TriggerFacade;
import com.trkgrn.jobscheduler.platform.common.dto.TriggerDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/triggers")
public class TriggerController {

    private final TriggerFacade triggerFacade;

    public TriggerController(TriggerFacade triggerFacade) {
        this.triggerFacade = triggerFacade;
    }

    @GetMapping
    public ResponseEntity<Result> list() {
        DataResult<List<TriggerDto>> result = triggerFacade.findAll();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result> get(@PathVariable Long id) {
        DataResult<TriggerDto> result = triggerFacade.findById(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/cron-job/{cronJobId}")
    public ResponseEntity<Result> getByCronJobId(@PathVariable Long cronJobId) {
        DataResult<List<TriggerDto>> result = triggerFacade.findByCronJobId(cronJobId);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping
    public ResponseEntity<Result> create(@RequestBody TriggerDto triggerDto) {
        DataResult<TriggerDto> result = triggerFacade.create(triggerDto);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result> update(@PathVariable Long id, @RequestBody TriggerDto triggerDto) {
        DataResult<TriggerDto> result = triggerFacade.update(id, triggerDto);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result> delete(@PathVariable Long id) {
        Result result = triggerFacade.deleteById(id);
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.NO_CONTENT : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Result> pause(@PathVariable Long id) {
        DataResult<TriggerDto> result = triggerFacade.pause(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Result> resume(@PathVariable Long id) {
        DataResult<TriggerDto> result = triggerFacade.resume(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/{id}/next-fire-time")
    public ResponseEntity<Result> getNextFireTime(@PathVariable Long id) {
        DataResult<Map<String, Object>> result = triggerFacade.getNextFireTime(id);
        if (!result.getSuccess() && result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @GetMapping("/ready-to-fire")
    public ResponseEntity<Result> getReadyToFire() {
        DataResult<List<TriggerDto>> result = triggerFacade.findReadyToFire();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

    @PostMapping("/sync")
    public ResponseEntity<Result> syncTriggers() {
        DataResult<Map<String, Object>> result = triggerFacade.syncTriggers();
        return ResponseEntity.status(result.getSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
    }

}
