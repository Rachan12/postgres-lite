package com.postgresql.command;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.common.DataType;
import com.postgresql.model.Table;
import com.postgresql.model.Tuple;
import com.postgresql.storage.TableSerializer;

import java.util.ArrayList;
import java.util.List;

public class InsertCommand implements Command {
    private final String tableName;
    private final List<String> values;

    public InsertCommand(String tableName, List<String> values) {
        this.tableName = tableName;
        this.values = values;
    }

    @Override
    public void execute() {
        Table table = CatalogManager.getInstance().getTable(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableName);
        }

        List<ColumnMetadata> columns = table.getMetadata().getColumns();
        if (values.size() != columns.size()) {
            throw new IllegalArgumentException("Mismatch between values and columns");
        }

        List<Object> row = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            ColumnMetadata col = columns.get(i);
            String val = values.get(i);
            Object parsed = parseValue(col.getType(), val);
            row.add(parsed);
        }

        Tuple newTuple = new Tuple(row);
        newTuple.acquireWriteLock();
        try {
            table.insertTuple(newTuple);
        } finally {
            newTuple.releaseWriteLock();
        }

        try {
            TableSerializer.writeToDisk(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist table: " + e.getMessage(), e);
        }

        System.out.println("✅ Row inserted into '" + tableName + "'");
    }

    private Object parseValue(DataType type, String val) {
        if (val == null || val.trim().isEmpty() || val.equalsIgnoreCase("NULL")) {
            return null;
        }
        return switch (type) {
            case INT -> Integer.parseInt(val);
            case FLOAT -> Float.parseFloat(val);
            case BOOLEAN -> Boolean.parseBoolean(val);
            default -> val;
        };
    }
}
