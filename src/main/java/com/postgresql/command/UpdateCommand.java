package com.postgresql.command;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.model.Table;
import com.postgresql.storage.TableSerializer;
import com.postgresql.model.Tuple;
import java.util.Objects;

public class UpdateCommand implements Command {
    private final String tableName;
    private final String targetColumn;
    private final String newValue;
    private final String whereColumn;
    private final String whereValue;

    public UpdateCommand(String tableName, String targetColumn, String newValue, String whereColumn,
            String whereValue) {
        this.tableName = tableName;
        this.targetColumn = targetColumn;
        this.newValue = newValue;
        this.whereColumn = whereColumn;
        this.whereValue = whereValue;
    }

    @Override
    public void execute() {
        Table table = CatalogManager.getInstance().getTable(tableName);
        if (table == null) {
            System.out.println("❌ Table not found: " + tableName);
            return;
        }

        int updated = 0;
        for (Tuple tuple : table.getAllTuples()) {
            tuple.acquireWriteLock();
            try {
                int targetIndex = table.getMetadata().getColumnIndex(targetColumn);
                int whereIndex = table.getMetadata().getColumnIndex(whereColumn);
                Object parsedNewValue = parseValue(table, targetColumn, newValue);
                Object parsedWhereValue = parseValue(table, whereColumn, whereValue);
                if (Objects.equals(tuple.getValues().get(whereIndex), parsedWhereValue)) {
                    tuple.getValues().set(targetIndex, parsedNewValue);
                    updated++;
                }
            } finally {
                tuple.releaseWriteLock();
            }
        }

        // ✅ Persist updated table data to disk
        try {
            TableSerializer.writeToDisk(table); // Save actual data
            CatalogManager.getInstance().saveTable(table); // Save metadata if needed
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to persist table: " + e.getMessage(), e);
        }

        System.out.println("✅ Updated " + updated + " rows.");
    }

    // Helper to parse value for a column
    private Object parseValue(Table table, String columnName, String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        var column = table.getMetadata().getColumnByName(columnName);
        return switch (column.getType()) {
            case INT -> Integer.parseInt(value);
            case FLOAT -> Float.parseFloat(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}
