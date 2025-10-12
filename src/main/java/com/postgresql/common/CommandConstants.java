package com.postgresql.common;

public final class CommandConstants {
    public static final String CREATE = "create table";
    public static final String INSERT = "insert into";
    public static final String SELECT = "select";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete from";
    public static final String ALTER = "alter table";

    private CommandConstants() {
        // prevent instantiation
    }
}