package com.postgresql;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.common.DataType;
import com.postgresql.command.InsertCommand;
import com.postgresql.command.SelectCommand;
import com.postgresql.command.UpdateCommand;
import com.postgresql.model.Table;
import com.postgresql.storage.TableSerializer;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseConcurrencyTest {
    private static final String TEST_TABLE = "test_users";
    private static final String DATA_DIR = "data";

    @BeforeAll
    void setup() throws Exception {
        // Create test table
        CatalogManager catalog = CatalogManager.getInstance();
        if (catalog.getTable(TEST_TABLE) == null) {
            catalog.createTable(TEST_TABLE, Arrays.asList(
                    new ColumnMetadata("id", DataType.INT),
                    new ColumnMetadata("name", DataType.STRING),
                    new ColumnMetadata("age", DataType.INT)));
        }
    }

    @AfterAll
    void cleanup() {
        // Remove test table file
        File f1 = new File(DATA_DIR + "/" + TEST_TABLE + ".table");
        File f2 = new File(DATA_DIR + "/" + TEST_TABLE + ".tbl");
        if (f1.exists()) f1.delete();
        if (f2.exists()) f2.delete();
    }

    @Test
    void testConcurrentReadAndWrite() throws Exception {
        // Insert initial data
        new InsertCommand(TEST_TABLE, Arrays.asList("3", "Charlie", "35")).execute();
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(1);
        Runnable reader = () -> {
            try {
                latch.await();
                System.setOut(new java.io.PrintStream(outContent));
                new SelectCommand(TEST_TABLE).execute();
            } catch (Exception ignored) {}
        };
        Runnable writer = () -> {
            try {
                latch.await();
                new UpdateCommand(TEST_TABLE, "name", "Updated", "id", "3").execute();
            } catch (Exception ignored) {}
        };
        // Launch readers and writers
        executor.submit(reader);
        executor.submit(writer);
        executor.submit(reader);
        executor.submit(writer);
        latch.countDown(); // Start all threads
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }
        System.setOut(originalOut);
        // Check update
        Table table = TableSerializer.readFromDisk(TEST_TABLE);
        boolean found = table.getAllTuples().stream().anyMatch(t -> t.getValues().get(1).equals("Updated"));
        assertTrue(found, "Update should be visible after concurrent execution");
        String output = outContent.toString();
        assertTrue(output.contains("Charlie") || output.contains("Updated"), "Output should contain 'Charlie' or 'Updated'");
    }
} 