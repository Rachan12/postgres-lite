package com.postgresql.catalog;

import com.postgresql.cli.ConsoleUI;
import com.postgresql.exception.InvalidSyntaxException;
import com.postgresql.model.Table;
import com.postgresql.model.Tuple;
import com.postgresql.storage.TableHeap;
import com.postgresql.storage.TableSerializer;

import java.io.*;
import java.util.*;

public class CatalogManager {
    private static final CatalogManager INSTANCE = new CatalogManager();
    private static final String DB_PATH = "./data/";
    private final Map<String, Table> tables = new HashMap<>();

    private CatalogManager() {
        loadTablesFromDisk(); // 🔁 Load tables on startup
    }

    public static CatalogManager getInstance() {
        return INSTANCE;
    }

    public void createTable(String name, List<ColumnMetadata> columns) {
        if (tables.containsKey(name)) {
            throw new RuntimeException("Table already exists: " + name);
        }

        TableMetadata metadata = new TableMetadata(name, columns);
        TableHeap heap = new TableHeap();
        Table table = new Table(name, metadata, heap);
        tables.put(name, table);
        saveTable(table); // 💾 Persist to disk
    }

    public List<String> listTables() {
        return new ArrayList<>(tables.keySet());
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    // ------------------------
    // Persistence-related methods
    // ------------------------

    public void saveTable(Table table) {
        try {
            File dir = new File(DB_PATH);
            if (!dir.exists())
                dir.mkdirs();

            FileOutputStream fos = new FileOutputStream(DB_PATH + table.getName() + ".table");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(table);
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save table: " + table.getName(), e);
        }
    }

    private void loadTablesFromDisk() {
        File dir = new File(DB_PATH);
        if (!dir.exists() || !dir.isDirectory())
            return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".table"));
        if (files == null)
            return;

        for (File file : files) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Table table = (Table) ois.readObject();
                tables.put(table.getName(), table);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to load table from file: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    public void addColumn(String tableName, ColumnMetadata newColumn) throws IOException {
        Table table = tables.get(tableName);

        if (table == null) {
            throw new InvalidSyntaxException("Table not found: " + tableName);
        }

        try {
            // ✅ Add to metadata (updates column list and maps)
            table.getMetadata().addColumn(newColumn);
        } catch (IllegalArgumentException e) {
            throw new InvalidSyntaxException(e.getMessage());
        }

        // ✅ Add null to each existing row to match the new column
        for (Tuple tuple : table.getAllTuples()) {
            tuple.getValues().add(null);
        }

        // ✅ Save updated table
        TableSerializer.writeToDisk(table);

        try {
            Table updatedTable = TableSerializer.readFromDisk(tableName);
            tables.put(tableName, updatedTable);
        } catch (IOException | ClassNotFoundException e) {
            ConsoleUI.printError("❌ Failed to reload updated table: " + e.getMessage());
        }
    }

}
