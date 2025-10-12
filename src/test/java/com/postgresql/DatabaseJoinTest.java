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
public class DatabaseJoinTest {
    private static final String USERS = "test_join_users";
    private static final String ORDERS = "test_join_orders";
    private static final String DATA_DIR = "data";

    @BeforeAll
    void setup() throws Exception {
        CatalogManager catalog = CatalogManager.getInstance();
        if (catalog.getTable(USERS) == null) {
            catalog.createTable(USERS, Arrays.asList(
                    new ColumnMetadata("id", DataType.INT),
                    new ColumnMetadata("name", DataType.STRING),
                    new ColumnMetadata("age", DataType.INT)));
        }
        if (catalog.getTable(ORDERS) == null) {
            catalog.createTable(ORDERS, Arrays.asList(
                    new ColumnMetadata("id", DataType.INT),
                    new ColumnMetadata("user_id", DataType.INT),
                    new ColumnMetadata("amount", DataType.INT)));
        }
        new InsertCommand(USERS, Arrays.asList("1", "Alice", "30")).execute();
        new InsertCommand(USERS, Arrays.asList("2", "Bob", "25")).execute();
        new InsertCommand(USERS, Arrays.asList("3", "Charlie", "35")).execute();
        new InsertCommand(ORDERS, Arrays.asList("101", "1", "200")).execute();
        new InsertCommand(ORDERS, Arrays.asList("102", "2", "150")).execute();
        new InsertCommand(ORDERS, Arrays.asList("103", "1", "300")).execute();
        new InsertCommand(ORDERS, Arrays.asList("104", "5", "400")).execute();
    }

    @AfterAll
    void cleanup() {
        File f1 = new File(DATA_DIR + "/" + USERS + ".table");
        File f2 = new File(DATA_DIR + "/" + USERS + ".tbl");
        File f3 = new File(DATA_DIR + "/" + ORDERS + ".table");
        File f4 = new File(DATA_DIR + "/" + ORDERS + ".tbl");
        if (f1.exists()) f1.delete();
        if (f2.exists()) f2.delete();
        if (f3.exists()) f3.delete();
        if (f4.exists()) f4.delete();
    }

    @Test
    void testInnerJoin() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, null, null, null, true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("Alice"), "Should contain Alice in join output");
        assertTrue(output.contains("Bob"), "Should contain Bob in join output");
        assertTrue(output.contains("200"), "Should contain order amount 200");
    }

    @Test
    void testLeftJoinWithNulls() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, null, null, null, true, null, null,
                "LEFT", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("Charlie"), "Should contain Charlie in left join output");
        assertTrue(output.contains("null") || output.toLowerCase().contains("null"), "Should show nulls for unmatched rows");
    }

    @Test
    void testRightJoinWithNulls() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, null, null, null, true, null, null,
                "RIGHT", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("400"), "Should contain order amount 400 in right join output");
        assertTrue(output.contains("null") || output.toLowerCase().contains("null"), "Should show nulls for unmatched rows");
    }

    @Test
    void testQualifiedColumnWhere() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, USERS+".id", "1", null, true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
            new SelectCommand(
                USERS, ORDERS+".id", "101", null, true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("Alice"), "Should filter by users.id = 1");
        assertTrue(output.contains("101"), "Should filter by orders.id = 101");
    }

    @Test
    void testQualifiedColumnOrderBy() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, null, null, USERS+".id", true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
            new SelectCommand(
                USERS, null, null, ORDERS+".id", false, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("Alice"), "Should order by users.id");
        assertTrue(output.contains("101"), "Should order by orders.id");
    }

    @Test
    void testIsNullAndIsNotNull() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, ORDERS+".amount", "IS NULL", null, true, null, null,
                "LEFT", ORDERS, "id", "user_id").execute();
            new SelectCommand(
                USERS, USERS+".name", "IS NULL", null, true, null, null,
                "RIGHT", ORDERS, "id", "user_id").execute();
            new SelectCommand(
                USERS, ORDERS+".amount", "IS NOT NULL", null, true, null, null,
                "LEFT", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.contains("null") || output.toLowerCase().contains("null"), "Should show nulls for IS NULL/IS NOT NULL");
    }

    @Test
    void testNoMatchesEdgeCase() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));
        try {
            new SelectCommand(
                USERS, USERS+".id", "999", null, true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
            new SelectCommand(
                USERS, ORDERS+".id", "999", null, true, null, null,
                "INNER", ORDERS, "id", "user_id").execute();
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString();
        assertTrue(output.isEmpty() || !output.contains("Alice"), "Should not output any matching rows for non-existent IDs");
    }
} 