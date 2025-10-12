package com.postgresql;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.common.DataType;
import com.postgresql.model.Tuple;
import com.postgresql.model.Table;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CatalogManager catalog = CatalogManager.getInstance();

        catalog.createTable("users", Arrays.asList(
                new ColumnMetadata("id", DataType.INT),
                new ColumnMetadata("name", DataType.STRING),
                new ColumnMetadata("email", DataType.STRING)));

        System.out.println("Tables in catalog:");
        catalog.listTables().forEach(System.out::println);

        // Insert dummy data
        Table usersTable = catalog.getTable("users");

        usersTable.insertTuple(new Tuple(Arrays.asList(1, "Rachana", "rachana@example.com")));
        usersTable.insertTuple(new Tuple(Arrays.asList(2, "Yashvi", "yashvi@example.com")));

        // Read and print tuples
        System.out.println("\nData in 'users' table:");
        List<Tuple> tuples = usersTable.getAllTuples();
        for (Tuple t : tuples) {
            System.out.println(t);
        }
    }
}
