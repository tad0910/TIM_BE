package com.tim.appTim.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) // Annotation này giúp trả về 422
public class UnprocessableException extends RuntimeException { // Quan trọng: extends RuntimeException

    public UnprocessableException(String message) {
        super(message);
    }

    public UnprocessableException(String message, Throwable cause) {
        super(message, cause);
    }
}
