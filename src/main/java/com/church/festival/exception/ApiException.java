package com.church.festival.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(String message) {
        this(message, 500);
    }

    public ApiException(String message, int status) {
        super(message);
        this.status = status;
    }
}
