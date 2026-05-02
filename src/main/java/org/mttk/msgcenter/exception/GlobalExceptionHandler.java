package org.mttk.msgcenter.exception;

import lombok.extern.slf4j.Slf4j;
import org.mttk.msgcenter.model.vo.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        ResponseEntity<?> response = new ResponseEntity();
        response.setCode(e.getCode());
        response.setMsg(e.getMessage());
        return response;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        ResponseEntity<?> response = new ResponseEntity();
        response.setCode(500);
        response.setMsg(e.getMessage());
        return response;
    }
}
