package com.jean202.cardmizer.api.config;

import com.jean202.cardmizer.core.application.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return new ApiErrorResponse(
                "NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
            );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiFieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        return new ApiErrorResponse(
                "BAD_REQUEST",
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            DateTimeParseException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(Exception exception, HttpServletRequest request) {
        return new ApiErrorResponse(
                "BAD_REQUEST",
                exception.getMessage(),
                request.getRequestURI(),
                List.of()
        );
    }

    public record ApiErrorResponse(
            String code,
            String message,
            String path,
            List<ApiFieldErrorResponse> fieldErrors
    ) {
    }

    public record ApiFieldErrorResponse(
            String field,
            String message,
            String rejectedValue
    ) {
    }

    private ApiFieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ApiFieldErrorResponse(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                stringify(fieldError.getRejectedValue())
        );
    }

    private String stringify(Object rejectedValue) {
        return rejectedValue == null ? null : String.valueOf(rejectedValue);
    }
}
