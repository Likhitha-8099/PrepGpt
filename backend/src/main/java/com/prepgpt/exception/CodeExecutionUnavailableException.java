package com.prepgpt.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CodeExecutionUnavailableException extends RuntimeException {
    public CodeExecutionUnavailableException(String message) {
        super(message);
    }
    public CodeExecutionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
