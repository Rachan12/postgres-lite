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
public class DatabaseSelectAdvancedTest {
    private static final String ADV_TABLE = "test_select_adv";
    private static final String DATA_DIR = "data";

    @BeforeAll
    void setup() throws Exception {
        CatalogManager catalog = CatalogManager.getInstance();
        if (catalog.getTable(ADV_TABLE) == null) {
            catalog.createTable(ADV_TABLE, Arrays.asList(
                    new ColumnMetadata("id", DataType.INT),
                    new ColumnMetadata("name", DataType.STRING),
                    new ColumnMetadata("age", DataType.INT)));
        }
        new InsertCommand(ADV_TABLE, Arrays.asList("1", "Alice", "30")).execute();
        new InsertCommand(ADV_TABLE, Arrays.asList("2", "Bob", "25")).execute();
        new InsertCommand(ADV_TABLE, Arrays.asList("3", "Charlie", "35")).execute();
    }

    @AfterAll
    void cleanup() {
        File f1 = new File(DATA_DIR + "/" + ADV_TABLE + ".table");
        File f2 = new File(DATA_DIR + "/" + ADV_TABLE + ".tbl");
        if (f1.exists()) f1.delete();
        if (f2.exists()) f2.delete();
    }

    @Test
    void testOrderByAsc() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(ADV_TABLE, null, null, "id", true, null, null).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        int idxAlice = output.indexOf("Alice");
        int idxBob = output.indexOf("Bob");
        int idxCharlie = output.indexOf("Charlie");
        assertTrue(idxAlice < idxBob && idxBob < idxCharlie, "Order should be Alice, Bob, Charlie");
    }

    @Test
    void testOrderByDesc() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(ADV_TABLE, null, null, "id", false, null, null).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        int idxCharlie = output.indexOf("Charlie");
        int idxBob = output.indexOf("Bob");
        int idxAlice = output.indexOf("Alice");
        assertTrue(idxCharlie < idxBob && idxBob < idxAlice, "Order should be Charlie, Bob, Alice");
    }

    @Test
    void testLimit() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(ADV_TABLE, null, null, null, true, 2, null).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        int count = (output.split("Alice", -1).length - 1) + (output.split("Bob", -1).length - 1) + (output.split("Charlie", -1).length - 1);
        assertEquals(2, count, "Should only output 2 rows");
    }

    @Test
    void testOffset() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(ADV_TABLE, null, null, null, true, null, 1).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertFalse(output.contains("Alice"), "Alice should be skipped due to offset");
        assertTrue(output.contains("Bob") || output.contains("Charlie"), "Should contain Bob or Charlie");
    }

    @Test
    void testLimitAndOffset() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(ADV_TABLE, null, null, null, true, 1, 1).execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        int count = (output.split("Alice", -1).length - 1) + (output.split("Bob", -1).length - 1) + (output.split("Charlie", -1).length - 1);
        assertEquals(1, count, "Should only output 1 row");
    }
} 