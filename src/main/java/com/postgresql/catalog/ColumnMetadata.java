package com.postgresql.catalog;

import com.postgresql.common.DataType;

import java.io.Serializable;

public class ColumnMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final DataType type;

    public ColumnMetadata(String name, DataType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }
}
