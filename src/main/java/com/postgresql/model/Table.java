package com.postgresql.model;

import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.catalog.TableMetadata;
import com.postgresql.storage.TableHeap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final TableMetadata metadata;
    private final TableHeap tableHeap;

    public Table(String name, TableMetadata metadata, TableHeap tableHeap) {
        this.name = name;
        this.metadata = metadata;
        this.tableHeap = tableHeap;
    }

    public String getName() {
        return name;
    }

    public TableMetadata getMetadata() {
        return metadata;
    }

    public TableHeap getTableHeap() {
        return tableHeap;
    }

    public void insertTuple(Tuple tuple) {
        tableHeap.insertTuple(tuple);
    }

    public List<Tuple> getAllTuples() {
        return tableHeap.scanAllTuples();
    }

    public int getRowCount() {
        return tableHeap.scanAllTuples().size();
    }

    public int updateTuples(String targetColumn, String newValue, String whereColumn, String whereValue) {
        int targetIndex = metadata.getColumnIndex(targetColumn);
        int whereIndex = metadata.getColumnIndex(whereColumn);

        Object parsedWhereValue = parseValue(whereColumn, whereValue);
        Object parsedNewValue = parseValue(targetColumn, newValue);

        List<Tuple> tuples = tableHeap.scanAllTuples();
        int updated = 0;

        for (Tuple tuple : tuples) {
            Object actualValue = tuple.getValues().get(whereIndex);
            if (Objects.equals(actualValue, parsedWhereValue)) {
                tuple.getValues().set(targetIndex, parsedNewValue);
                updated++;
            }
        }

        return updated;
    }

    public int deleteTuples(String whereColumn, String whereValue) {
        int whereIndex = metadata.getColumnIndex(whereColumn);
        Object parsedWhereValue = parseValue(whereColumn, whereValue);

        List<Tuple> tuples = tableHeap.scanAllTuples();
        List<Tuple> toRemove = new ArrayList<>();

        for (Tuple tuple : tuples) {
            Object actualValue = tuple.getValues().get(whereIndex);
            if (Objects.equals(actualValue, parsedWhereValue)) {
                toRemove.add(tuple);
            }
        }

        tableHeap.deleteTuples(toRemove);
        return toRemove.size();
    }

    private Object parseValue(String columnName, String value) {
        if (value == null || value.trim().isEmpty())
            return null;

        ColumnMetadata column = metadata.getColumnByName(columnName);
        return switch (column.getType()) {
            case INT -> Integer.parseInt(value);
            case FLOAT -> Float.parseFloat(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}
