package com.tim.appTim.exception;

// Sử dụng RuntimeException để không cần try-catch và cho phép Spring xử lý
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}