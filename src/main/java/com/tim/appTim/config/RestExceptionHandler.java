package com.tim.appTim.config;

import com.tim.appTim.dto.ErrorResponse;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class) // <-- Bắt exception cụ thể này
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(), // <-- Mã lỗi 404
                "Not Found",                  // <-- Tên lỗi
                ex.getMessage(),              // <-- Message từ exception
                request.getDescription(false).replace("uri=", "") // <-- Path
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error); // <-- Trả về response 404
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                ex.getMessage() == null ? "Authentication failed" : ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * THÊM PHƯƠNG THỨC NÀY VÀO
     * Nó cụ thể hơn Exception.class nên sẽ được ưu tiên cho lỗi 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(Instant.now(), HttpStatus.FORBIDDEN.value(), "Forbidden",
                ex.getMessage() == null ? "Bạn không có quyền truy cập tài nguyên này." : ex.getMessage(),
                request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
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