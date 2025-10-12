package com.postgresql.command;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.cli.ConsoleUI;
import com.postgresql.common.DataType;

import java.util.List;

public class CreateTableCommand implements Command {
    private final String tableName;
    private final List<ColumnMetadata> columns;

    public CreateTableCommand(String tableName, List<ColumnMetadata> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    @Override
    public void execute() {
        CatalogManager.getInstance().createTable(tableName, columns);
        ConsoleUI.printSuccess("🎉 Table '" + tableName + "' created successfully!");

        // Pretty print schema
        ConsoleUI.printSchema(tableName, columns);
    }
}