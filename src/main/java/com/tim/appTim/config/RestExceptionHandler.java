package com.tim.appTim.config;

import com.tim.appTim.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                ex.getMessage() == null ? "Authentication failed" : ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage() == null ? "Unexpected error" : ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}