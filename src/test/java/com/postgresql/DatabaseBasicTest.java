package com.postgresql;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.common.DataType;
import com.postgresql.command.InsertCommand;
import com.postgresql.command.SelectCommand;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseBasicTest {
    private static final String BASIC_TABLE = "test_basic";
    private static final String DATA_DIR = "data";

    @BeforeAll
    void setup() throws Exception {
        CatalogManager catalog = CatalogManager.getInstance();
        if (catalog.getTable(BASIC_TABLE) == null) {
            catalog.createTable(BASIC_TABLE, Arrays.asList(
                    new ColumnMetadata("id", DataType.INT),
                    new ColumnMetadata("name", DataType.STRING),
                    new ColumnMetadata("age", DataType.INT)));
        }
    }

    @AfterAll
    void cleanup() {
        File f1 = new File(DATA_DIR + "/" + BASIC_TABLE + ".table");
        File f2 = new File(DATA_DIR + "/" + BASIC_TABLE + ".tbl");
        if (f1.exists()) f1.delete();
        if (f2.exists()) f2.delete();
    }

    @Test
    void testInsertAndSelect() {
        new InsertCommand(BASIC_TABLE, Arrays.asList("1", "Alice", "30")).execute();
        new InsertCommand(BASIC_TABLE, Arrays.asList("2", "Bob", "25")).execute();

        // Capture System.out
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(BASIC_TABLE).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("Alice"), "Output should contain 'Alice'");
        assertTrue(output.contains("Bob"), "Output should contain 'Bob'");
    }
} 