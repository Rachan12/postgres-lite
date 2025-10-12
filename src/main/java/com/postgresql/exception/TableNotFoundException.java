package com.postgresql.exception;

public class TableNotFoundException extends RuntimeException {
    public TableNotFoundException(String tableName) {
        super("Table not found: " + tableName);
    }
}
