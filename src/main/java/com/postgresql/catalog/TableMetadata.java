package com.postgresql.catalog;

import java.io.Serializable;
import java.util.*;

public class TableMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String tableName;
    private final List<ColumnMetadata> columns;

    private final Map<String, Integer> columnIndexMap;
    private final Map<String, ColumnMetadata> columnMetadataMap;

    public TableMetadata(String tableName, List<ColumnMetadata> columns) {
        this.tableName = tableName;
        this.columns = columns;
        this.columnIndexMap = new HashMap<>();
        this.columnMetadataMap = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            String nameLower = columns.get(i).getName().toLowerCase();
            columnIndexMap.put(nameLower, i);
            columnMetadataMap.put(nameLower, columns.get(i));
        }
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public int getColumnIndex(String columnName) {
        Integer index = columnIndexMap.get(columnName.toLowerCase());
        if (index == null) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return index;
    }

    public ColumnMetadata getColumnByName(String name) {
        ColumnMetadata col = columnMetadataMap.get(name.toLowerCase());
        if (col == null) {
            throw new IllegalArgumentException("Column not found: " + name);
        }
        return col;
    }

    public void addColumn(ColumnMetadata newColumn) {
        String nameLower = newColumn.getName().toLowerCase();
        if (columnIndexMap.containsKey(nameLower)) {
            throw new IllegalArgumentException("Column already exists: " + newColumn.getName());
        }

        columns.add(newColumn);
        columnIndexMap.put(nameLower, columns.size() - 1);
        columnMetadataMap.put(nameLower, newColumn);
    }
}
