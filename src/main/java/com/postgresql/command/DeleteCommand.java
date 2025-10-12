package com.postgresql.command;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.model.Table;
import com.postgresql.storage.TableSerializer;
import com.postgresql.model.Tuple;

import java.util.ArrayList;
import java.util.List;

public class DeleteCommand implements Command {
    private final String tableName;
    private final String whereColumn;
    private final String whereValue;

    public DeleteCommand(String tableName, String whereColumn, String whereValue) {
        this.tableName = tableName;
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

        int deletedCount = 0;
        List<Tuple> toRemove = new ArrayList<>();
        for (Tuple tuple : table.getTableHeap().scanAllTuples()) {
            tuple.acquireWriteLock();
            try {
                Object actualValue = tuple.getValue(whereColumn, table.getMetadata());
                if (actualValue != null && actualValue.toString().equalsIgnoreCase(whereValue)) {
                    toRemove.add(tuple);
                    deletedCount++;
                }
            } finally {
                tuple.releaseWriteLock();
            }
        }
        table.getTableHeap().deleteTuples(toRemove);

        // ✅ Persist updated table data to disk
        try {
            TableSerializer.writeToDisk(table); // Save actual data
            CatalogManager.getInstance().saveTable(table); // Save metadata if needed
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to persist table: " + e.getMessage(), e);
        }

        System.out.println("✅ Deleted " + deletedCount + " rows.");
    }
}
