package com.jean202.cardmizer.api.config

import com.jean202.cardmizer.core.application.ResourceNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.format.DateTimeParseException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: ResourceNotFoundException, request: HttpServletRequest) =
        ApiErrorResponse("NOT_FOUND", exception.message, request.requestURI, emptyList())

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(exception: MethodArgumentNotValidException, request: HttpServletRequest): ApiErrorResponse {
        val fieldErrors = exception.bindingResult.fieldErrors.map { toFieldErrorResponse(it) }
        return ApiErrorResponse("BAD_REQUEST", "Validation failed", request.requestURI, fieldErrors)
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        DateTimeParseException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class,
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(exception: Exception, request: HttpServletRequest) =
        ApiErrorResponse("BAD_REQUEST", exception.message, request.requestURI, emptyList())

    data class ApiErrorResponse(
        val code: String,
        val message: String?,
        val path: String,
        val fieldErrors: List<ApiFieldErrorResponse>,
    )

    data class ApiFieldErrorResponse(
        val field: String,
        val message: String?,
        val rejectedValue: String?,
    )

    private fun toFieldErrorResponse(fieldError: FieldError) = ApiFieldErrorResponse(
        field = fieldError.field,
        message = fieldError.defaultMessage,
        rejectedValue = fieldError.rejectedValue?.toString(),
    )
}
