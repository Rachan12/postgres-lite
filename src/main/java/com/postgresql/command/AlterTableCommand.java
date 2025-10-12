package com.postgresql.command;

import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.cli.ConsoleUI;
import com.postgresql.common.DataType;

import java.io.IOException;

public class AlterTableCommand implements Command {
    private final String tableName;
    private final String columnName;
    private final DataType dataType;

    public AlterTableCommand(String tableName, String columnName, DataType dataType) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.dataType = dataType;
    }

    @Override
    public void execute() {
        try {
            CatalogManager.getInstance()
                    .addColumn(tableName, new ColumnMetadata(columnName, dataType));
            ConsoleUI.printSuccess("➕ Column '" + columnName + "' added to table '" + tableName + "'.");
        } catch (RuntimeException e) {
            ConsoleUI.printError("❌ Failed to alter table: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

