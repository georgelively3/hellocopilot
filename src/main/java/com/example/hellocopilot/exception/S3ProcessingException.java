package com.example.hellocopilot.exception;

public class S3ProcessingException extends RuntimeException {

    public S3ProcessingException(String message) {
        super(message);
    }

    public S3ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
