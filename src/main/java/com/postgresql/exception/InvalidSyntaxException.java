package com.postgresql.exception;

public class InvalidSyntaxException extends RuntimeException {
    public InvalidSyntaxException(String msg) {
        super(msg);
    }
}
