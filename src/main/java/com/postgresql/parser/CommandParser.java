package com.postgresql.parser;

import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.cli.ConsoleUI;
import com.postgresql.command.*;
import com.postgresql.common.CommandConstants;
import com.postgresql.common.DataType;
import com.postgresql.exception.InvalidSyntaxException;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {
    private static final Map<String, Function<String, Command>> parserMap = new LinkedHashMap<>();

    static {
        parserMap.put(CommandConstants.ALTER, CommandParser::parseAlterTable);
        parserMap.put(CommandConstants.CREATE, CommandParser::parseCreateTable);
        parserMap.put(CommandConstants.INSERT, CommandParser::parseInsert);
        parserMap.put(CommandConstants.SELECT, CommandParser::parseSelect);
        parserMap.put(CommandConstants.UPDATE, CommandParser::parseUpdate);
        parserMap.put(CommandConstants.DELETE, CommandParser::parseDelete);
    }

    public static Command parse(String input) {
        String trimmedInput = input.trim();
        if (trimmedInput.isEmpty()) {
            return null; // No command for empty input
        }
        String lower = trimmedInput.toLowerCase();

        return parserMap.entrySet()
                .stream()
                .filter(entry -> lower.startsWith(entry.getKey()))
                .map(entry -> entry.getValue().apply(trimmedInput))
                .findFirst()
                .orElseThrow(() -> {
                    ConsoleUI.printError("Unsupported command: " + input);
                    return new InvalidSyntaxException("Unsupported command: " + input);
                });
    }

    // -------------------------------
    // CREATE TABLE users (id INT, name STRING)
    private static Command parseCreateTable(String input) {
        try {
            String[] parts = input.split("\\(", 2);
            String tableName = parts[0].trim().split("\\s+")[2];
            String[] columnDefs = parts[1].replaceAll("\\)", "").split(",");

            List<ColumnMetadata> columns = new ArrayList<>();
            for (String def : columnDefs) {
                String[] colParts = def.trim().split("\\s+");
                if (colParts.length != 2) {
                    ConsoleUI.printUnsupportedCommand(input);
                    throw new InvalidSyntaxException("Invalid column: " + def);
                }

                String columnName = colParts[0].trim();
                String dataTypeStr = colParts[1].trim().toUpperCase();

                try {
                    DataType dataType = DataType.valueOf(dataTypeStr);
                    columns.add(new ColumnMetadata(columnName, dataType));
                } catch (IllegalArgumentException e) {
                    ConsoleUI.printUnsupportedCommand(input);
                    throw new InvalidSyntaxException("Unsupported data type: " + dataTypeStr);
                }
            }

            return new CreateTableCommand(tableName, columns);

        } catch (Exception e) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Syntax error in CREATE TABLE: " + e.getMessage());
        }
    }

    // -------------------------------
    // INSERT INTO users VALUES ('Alice', 1)
    private static Command parseInsert(String input) {
        try {
            String[] parts = input.split("(?i)values", 2);
            String tableName = parts[0].trim().split("\\s+")[2];
            String rawValues = parts[1].replaceAll("^\\s*\\(", "").replaceAll("\\)\\s*$", "");

            List<String> values = new ArrayList<>();
            Matcher matcher = Pattern.compile("'(.*?)'|([^,\\s]+)").matcher(rawValues);
            while (matcher.find()) {
                values.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
            }

            return new InsertCommand(tableName, values);

        } catch (Exception e) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Syntax error in INSERT INTO: " + e.getMessage());
        }
    }

    // -------------------------------
    // SELECT * FROM users
    private static Command parseSelect(String input) {
        String lower = input.toLowerCase();
        if (!lower.startsWith("select * from")) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Only SELECT * FROM supported currently.");
        }

        // Remove 'select * from'
        String rest = input.substring(14).trim();
        String whereColumn = null;
        String whereValue = null;
        String orderByColumn = null;
        boolean orderByAsc = true;
        Integer limit = null;
        Integer offset = null;

        // JOIN parsing (unchanged)
        String mainTable = null;
        String joinType = null;
        String joinTable = null;
        String joinLeftCol = null;
        String joinRightCol = null;

        Pattern joinPattern = Pattern.compile(
            "^(\\w+)(?:\\s+(inner|left|right)\\s+join\\s+(\\w+)\\s+on\\s+(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+))?(.*)$",
            Pattern.CASE_INSENSITIVE);
        Matcher joinMatcher = joinPattern.matcher(rest);
        String afterJoin = null;
        if (joinMatcher.matches()) {
            mainTable = joinMatcher.group(1);
            if (joinMatcher.group(2) != null) {
                joinType = joinMatcher.group(2).toUpperCase();
                joinTable = joinMatcher.group(3);
                String leftTable = joinMatcher.group(4);
                joinLeftCol = joinMatcher.group(5);
                String rightTable = joinMatcher.group(6);
                joinRightCol = joinMatcher.group(7);
                if (!(mainTable.equalsIgnoreCase(leftTable) && joinTable.equalsIgnoreCase(rightTable))) {
                    ConsoleUI.printUnsupportedCommand(input);
                    throw new InvalidSyntaxException("ON clause table names must match FROM/JOIN tables");
                }
            }
            afterJoin = joinMatcher.group(8) != null ? joinMatcher.group(8).trim() : "";
        } else {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Invalid SELECT/JOIN syntax.");
        }

        // Enhanced clause pattern: support qualified columns and IS NULL/IS NOT NULL
        Pattern clausePattern = Pattern.compile(
            "(?:where\\s+([\\w]+\\.?[\\w]*)\\s*(=|is)\\s*('?(.*?)'?)?)?(?:\\s+order\\s+by\\s+([\\w]+\\.?[\\w]*)(?:\\s+(asc|desc))?)?(?:\\s+limit\\s+(\\d+))?(?:\\s+offset\\s+(\\d+))?\\s*$",
            Pattern.CASE_INSENSITIVE);
        Matcher clauseMatcher = clausePattern.matcher(afterJoin);
        if (clauseMatcher.find()) {
            if (clauseMatcher.group(1) != null && clauseMatcher.group(2) != null) {
                whereColumn = clauseMatcher.group(1).trim();
                String op = clauseMatcher.group(2).trim().toUpperCase();
                String val = clauseMatcher.group(3) != null ? clauseMatcher.group(3).trim() : null;
                if (op.equals("IS")) {
                    if (val != null && val.equalsIgnoreCase("NULL")) {
                        whereValue = "IS NULL";
                    } else if (val != null && val.equalsIgnoreCase("NOT NULL")) {
                        whereValue = "IS NOT NULL";
                    } else {
                        ConsoleUI.printUnsupportedCommand(input);
                        throw new InvalidSyntaxException("Invalid IS NULL/IS NOT NULL syntax");
                    }
                } else if (op.equals("=")) {
                    if (val != null && val.startsWith("'")) val = val.replaceAll("^'(.*)'$", "$1");
                    whereValue = val;
                }
            }
            if (clauseMatcher.group(5) != null) {
                orderByColumn = clauseMatcher.group(5).trim();
                if (clauseMatcher.group(6) != null) {
                    orderByAsc = !clauseMatcher.group(6).equalsIgnoreCase("desc");
                }
            }
            if (clauseMatcher.group(7) != null) {
                limit = Integer.parseInt(clauseMatcher.group(7));
            }
            if (clauseMatcher.group(8) != null) {
                offset = Integer.parseInt(clauseMatcher.group(8));
            }
        }

        return new SelectCommand(mainTable, whereColumn, whereValue, orderByColumn, orderByAsc, limit, offset,
            joinType, joinTable, joinLeftCol, joinRightCol);
    }

    // -------------------------------
    // UPDATE users SET name = 'Bob' WHERE id = 1
    private static Command parseUpdate(String input) {
        try {
            Pattern pattern = Pattern.compile(
                    "(?i)^update\\s+(\\w+)\\s+set\\s+(\\w+)\\s*=\\s*('?[^']*'?)\\s+where\\s+(\\w+)\\s*=\\s*('?[^']*'?)$",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input.trim());

            if (!matcher.matches()) {
                ConsoleUI.printUnsupportedCommand(input);
                throw new InvalidSyntaxException("Invalid UPDATE syntax");
            }

            String tableName = matcher.group(1).trim();
            String targetColumn = matcher.group(2).trim();
            String newValue = unquote(matcher.group(3).trim());
            String whereColumn = matcher.group(4).trim();
            String whereValue = unquote(matcher.group(5).trim());

            return new UpdateCommand(tableName, targetColumn, newValue, whereColumn, whereValue);

        } catch (Exception e) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Syntax error in UPDATE: " + e.getMessage());
        }
    }

    // -------------------------------
    // DELETE FROM users WHERE id = 1
    private static Command parseDelete(String input) {
        // Normalize and remove the command prefix
        String lower = input.toLowerCase().trim();
        if (!lower.startsWith("delete from")) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Invalid DELETE syntax");
        }

        // Example: DELETE FROM users WHERE id = 1
        String[] parts = input.split("(?i)where");
        if (parts.length != 2) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Missing WHERE clause in DELETE");
        }

        String[] deleteParts = parts[0].trim().split("\\s+");
        if (deleteParts.length < 3) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Missing table name in DELETE");
        }

        String tableName = deleteParts[2].trim();

        String[] conditionParts = parts[1].trim().split("=");
        if (conditionParts.length != 2) {
            ConsoleUI.printUnsupportedCommand(input);
            throw new InvalidSyntaxException("Invalid WHERE clause in DELETE");
        }

        String column = conditionParts[0].trim();
        String value = conditionParts[1].trim().replaceAll("'", "");

        return new DeleteCommand(tableName, column, value);
    }

    private static String unquote(String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Command parseAlterTable(String input) {
        try {
            String[] tokens = input.trim().split("\\s+");

            // Expected format: ALTER TABLE <table> ADD COLUMN <column> <type>
            if (tokens.length != 7 ||
                    !tokens[0].equalsIgnoreCase("alter") ||
                    !tokens[1].equalsIgnoreCase("table") ||
                    !tokens[3].equalsIgnoreCase("add") ||
                    !tokens[4].equalsIgnoreCase("column")) {
                ConsoleUI.printError("Syntax must be: ALTER TABLE <table> ADD COLUMN <column> <type>");
                throw new InvalidSyntaxException("Invalid ALTER TABLE syntax.");
            }

            String tableName = tokens[2];
            String columnName = tokens[5];
            String dataTypeStr = tokens[6].toUpperCase();

            DataType dataType;
            try {
                dataType = DataType.valueOf(dataTypeStr);
            } catch (IllegalArgumentException e) {
                ConsoleUI.printError("Unknown data type: '" + dataTypeStr + "'");
                throw new InvalidSyntaxException("Unsupported data type in ALTER TABLE.");
            }

            return new AlterTableCommand(tableName, columnName, dataType);

        } catch (InvalidSyntaxException e) {
            throw e; // Already handled with console message
        } catch (Exception e) {
            ConsoleUI.printError("General syntax error in ALTER TABLE: " + e.getMessage());
            throw new InvalidSyntaxException("ALTER TABLE parse error.");
        }
    }
}
