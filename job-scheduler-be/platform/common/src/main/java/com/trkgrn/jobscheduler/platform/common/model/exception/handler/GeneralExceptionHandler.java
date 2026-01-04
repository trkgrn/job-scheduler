package com.trkgrn.jobscheduler.platform.common.model.exception.handler;

import com.trkgrn.jobscheduler.platform.common.constants.ExceptionConstantCode;
import com.trkgrn.jobscheduler.platform.common.model.exception.*;
import com.trkgrn.jobscheduler.platform.common.model.result.ErrorDataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.ErrorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GeneralExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GeneralExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDataResult<Map<String, String>>> handle(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        LOG.warn("Validation errors: {}", errors);
        return new ResponseEntity<>(
                new ErrorDataResult<>(errors, "Invalid input"),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResult> handle(NotFoundException exception) {
        LOG.warn("Not found exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.NOT_FOUND_EXCEPTION, exception.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResult> handle(AlreadyExistsException exception) {
        LOG.warn("Already exists exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.ALREADY_EXISTS_EXCEPTION, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NotCreatedException.class)
    public ResponseEntity<ErrorResult> handle(NotCreatedException exception) {
        LOG.error("Not created exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.NOT_CREATED_EXCEPTION, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NotUpdatedException.class)
    public ResponseEntity<ErrorResult> handle(NotUpdatedException exception) {
        LOG.error("Not updated exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.NOT_UPDATED_EXCEPTION, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NotDeletedException.class)
    public ResponseEntity<ErrorResult> handle(NotDeletedException exception) {
        LOG.error("Not deleted exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.NOT_DELETED_EXCEPTION, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NotValidException.class)
    public ResponseEntity<ErrorResult> handle(NotValidException exception) {
        LOG.warn("Not valid exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.NOT_VALID_EXCEPTION, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResult> handle(IllegalArgumentException exception) {
        LOG.warn("Illegal argument exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.ILLEGAL_ARGUMENT, exception.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(JobAlreadyRunningException.class)
    public ResponseEntity<ErrorResult> handle(JobAlreadyRunningException exception) {
        LOG.warn("Job already running exception: {}", exception.getMessage());
        return new ResponseEntity<>(
                new ErrorResult(ExceptionConstantCode.ALREADY_EXISTS_EXCEPTION, exception.getMessage()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> handle(Exception exception) {
        LOG.error("Unexpected exception occurred", exception);
        return new ResponseEntity<>(
                new ErrorResult((long) HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                        exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}

