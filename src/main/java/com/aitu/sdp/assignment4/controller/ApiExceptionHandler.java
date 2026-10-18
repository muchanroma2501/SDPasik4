package com.aitu.sdp.assignment4.controller;

import com.aitu.sdp.assignment2.IncompatibleHardwareException;
import com.aitu.sdp.assignment4.diagnostics.BuildDiagnostics;
import com.aitu.sdp.assignment4.diagnostics.DiagnosticSeverity;
import com.aitu.sdp.assignment4.facade.BuildRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BuildRejectedException.class)
    public ResponseEntity<ApiError> buildRejected(BuildRejectedException exception) {
        boolean invalidSelection = exception.getDiagnostics().checks().stream()
                .filter(check -> check.severity()
                        == DiagnosticSeverity.ERROR)
                .anyMatch(check -> Set.of("required-fields", "platform-selection", "cpu-model",
                        "gpu-model", "motherboard-model", "ram", "storage", "powerSupply",
                        "coolingSystem").contains(check.id()));
        return ResponseEntity.status(invalidSelection ? HttpStatus.BAD_REQUEST
                        : HttpStatus.UNPROCESSABLE_ENTITY).body(
                new ApiError(exception.getMessage(), exception.getDiagnostics()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage(), null));
    }

    @ExceptionHandler(IncompatibleHardwareException.class)
    public ResponseEntity<ApiError> incompatibleHardware(IncompatibleHardwareException exception) {
        return ResponseEntity.unprocessableEntity().body(new ApiError(exception.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "Некорректный JSON или отсутствует обязательное тело запроса.", null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedContentType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                new ApiError("Поддерживается только JSON-запрос с Content-Type: application/json.", null));
    }

    public record ApiError(String message, BuildDiagnostics diagnostics) {
    }
}
