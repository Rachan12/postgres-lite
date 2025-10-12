package com.postgresql.storage;

import com.postgresql.model.Table;

import java.io.*;

public class TableSerializer {

    private static final String DATA_DIR = "data";

    public static void writeToDisk(Table table) throws IOException {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = DATA_DIR + "/" + table.getMetadata().getTableName() + ".tbl";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(table);
        }
    }

    public static Table readFromDisk(String tableName) throws IOException, ClassNotFoundException {
        String fileName = DATA_DIR + "/" + tableName + ".tbl";
        File file = new File(fileName);
        if (!file.exists()) {
            return null; // table not persisted yet
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Table) ois.readObject();
        }
    }
}
