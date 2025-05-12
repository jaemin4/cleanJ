package com.example.demo.support.exception;

import com.example.demo.support.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> bindException(BindException e) {
        String message = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        String fullMessages = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("BindException 발생: {}", fullMessages);

        return ApiResponse.fail(
                HttpStatus.BAD_REQUEST.value(),
                message
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleRuntimeException(RuntimeException e) {
        return ApiResponse.fail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 오류가 발생했습니다: " + e.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleException(Exception e) {
        return ApiResponse.fail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "예기치 못한 오류가 발생했습니다."
        );
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS) // 429
    public ApiResponse<Object> handleTooManyRequests(TooManyRequestsException e) {
        return ApiResponse.fail(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                e.getMessage()
        );
    }

}
