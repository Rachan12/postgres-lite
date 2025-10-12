package com.postgresql.command;

import com.postgresql.model.Table;
import com.postgresql.model.Tuple;
import com.postgresql.storage.TableSerializer;

import java.util.List;
import java.util.stream.Collectors;

public class SelectCommand implements Command {
    private final String tableName;
    private final String whereColumn;
    private final String whereValue;
    private final String orderByColumn;
    private final boolean orderByAsc;
    private final Integer limit;
    private final Integer offset;
    private final String joinType;
    private final String joinTable;
    private final String joinLeftCol;
    private final String joinRightCol;

    public SelectCommand(String tableName) {
        this(tableName, null, null);
    }

    public SelectCommand(String tableName, String whereColumn, String whereValue) {
        this(tableName, whereColumn, whereValue, null, true, null, null);
    }

    public SelectCommand(String tableName, String whereColumn, String whereValue, String orderByColumn, boolean orderByAsc, Integer limit, Integer offset) {
        this(tableName, whereColumn, whereValue, orderByColumn, orderByAsc, limit, offset, null, null, null, null);
    }

    public SelectCommand(String tableName, String whereColumn, String whereValue, String orderByColumn, boolean orderByAsc, Integer limit, Integer offset,
                        String joinType, String joinTable, String joinLeftCol, String joinRightCol) {
        this.tableName = tableName;
        this.whereColumn = whereColumn;
        this.whereValue = whereValue;
        this.orderByColumn = orderByColumn;
        this.orderByAsc = orderByAsc;
        this.limit = limit;
        this.offset = offset;
        this.joinType = joinType;
        this.joinTable = joinTable;
        this.joinLeftCol = joinLeftCol;
        this.joinRightCol = joinRightCol;
    }

    @Override
    public void execute() {
        try {
            if (joinType == null) {
                Table table = TableSerializer.readFromDisk(tableName);
                if (table == null) {
                    System.out.println("❌ Table not found on disk: " + tableName);
                    return;
                }
                printHeader(table.getMetadata().getColumns(), null);
                List<Tuple> tuples = table.getTableHeap().scanAllTuples();
                // Enhanced WHERE filter with qualified column support
                if (whereColumn != null && whereValue != null) {
                    int whereIdx = getQualifiedColumnIndex(whereColumn, table, null);
                    if (whereValue.equalsIgnoreCase("IS NULL")) {
                        tuples = tuples.stream()
                                .filter(tuple -> tuple.getValues().get(whereIdx) == null)
                                .collect(Collectors.toList());
                    } else if (whereValue.equalsIgnoreCase("IS NOT NULL")) {
                        tuples = tuples.stream()
                                .filter(tuple -> tuple.getValues().get(whereIdx) != null)
                                .collect(Collectors.toList());
                    } else {
                        tuples = tuples.stream()
                                .filter(tuple -> {
                                    Object value = tuple.getValues().get(whereIdx);
                                    return value != null && value.toString().equalsIgnoreCase(whereValue);
                                })
                                .collect(Collectors.toList());
                    }
                }
                // ORDER BY with qualified column support
                if (orderByColumn != null) {
                    int colIdx = getQualifiedColumnIndex(orderByColumn, table, null);
                    tuples = tuples.stream()
                            .sorted((a, b) -> {
                                Comparable va = (Comparable) a.getValues().get(colIdx);
                                Comparable vb = (Comparable) b.getValues().get(colIdx);
                                int cmp = 0;
                                if (va == null && vb == null) cmp = 0;
                                else if (va == null) cmp = -1;
                                else if (vb == null) cmp = 1;
                                else cmp = va.compareTo(vb);
                                return orderByAsc ? cmp : -cmp;
                            })
                            .collect(Collectors.toList());
                }
                if (offset != null && offset > 0) {
                    if (offset < tuples.size()) {
                        tuples = tuples.subList(offset, tuples.size());
                    } else {
                        tuples = List.of();
                    }
                }
                if (limit != null && limit >= 0 && limit < tuples.size()) {
                    tuples = tuples.subList(0, limit);
                }
                for (Tuple tuple : tuples) {
                    tuple.acquireReadLock();
                    try {
                        printRow(tuple);
                    } finally {
                        tuple.releaseReadLock();
                    }
                }
            } else {
                Table leftTable = TableSerializer.readFromDisk(tableName);
                Table rightTable = TableSerializer.readFromDisk(joinTable);
                if (leftTable == null || rightTable == null) {
                    System.out.println("❌ One or both tables not found on disk: " + tableName + ", " + joinTable);
                    return;
                }
                printHeader(leftTable.getMetadata().getColumns(), rightTable.getMetadata().getColumns());
                List<Tuple> leftTuples = leftTable.getTableHeap().scanAllTuples();
                List<Tuple> rightTuples = rightTable.getTableHeap().scanAllTuples();
                int leftIdx = leftTable.getMetadata().getColumnIndex(joinLeftCol);
                int rightIdx = rightTable.getMetadata().getColumnIndex(joinRightCol);
                List<List<Object>> joinedRows = new java.util.ArrayList<>();
                if ("INNER".equalsIgnoreCase(joinType)) {
                    for (Tuple l : leftTuples) {
                        Object lval = l.getValues().get(leftIdx);
                        for (Tuple r : rightTuples) {
                            Object rval = r.getValues().get(rightIdx);
                            if (lval != null && lval.equals(rval)) {
                                List<Object> row = new java.util.ArrayList<>();
                                row.addAll(l.getValues());
                                row.addAll(r.getValues());
                                joinedRows.add(row);
                            }
                        }
                    }
                } else if ("LEFT".equalsIgnoreCase(joinType)) {
                    for (Tuple l : leftTuples) {
                        Object lval = l.getValues().get(leftIdx);
                        boolean matched = false;
                        for (Tuple r : rightTuples) {
                            Object rval = r.getValues().get(rightIdx);
                            if (lval != null && lval.equals(rval)) {
                                List<Object> row = new java.util.ArrayList<>();
                                row.addAll(l.getValues());
                                row.addAll(r.getValues());
                                joinedRows.add(row);
                                matched = true;
                            }
                        }
                        if (!matched) {
                            List<Object> row = new java.util.ArrayList<>();
                            row.addAll(l.getValues());
                            for (int i = 0; i < rightTable.getMetadata().getColumns().size(); i++) row.add(null);
                            joinedRows.add(row);
                        }
                    }
                } else if ("RIGHT".equalsIgnoreCase(joinType)) {
                    for (Tuple r : rightTuples) {
                        Object rval = r.getValues().get(rightIdx);
                        boolean matched = false;
                        for (Tuple l : leftTuples) {
                            Object lval = l.getValues().get(leftIdx);
                            if (lval != null && lval.equals(rval)) {
                                List<Object> row = new java.util.ArrayList<>();
                                row.addAll(l.getValues());
                                row.addAll(r.getValues());
                                joinedRows.add(row);
                                matched = true;
                            }
                        }
                        if (!matched) {
                            List<Object> row = new java.util.ArrayList<>();
                            for (int i = 0; i < leftTable.getMetadata().getColumns().size(); i++) row.add(null);
                            row.addAll(r.getValues());
                            joinedRows.add(row);
                        }
                    }
                } else {
                    System.out.println("❌ Unsupported JOIN type: " + joinType);
                    return;
                }
                // Enhanced WHERE on joined result (if present)
                if (whereColumn != null && whereValue != null) {
                    int whereIdx = getQualifiedColumnIndex(whereColumn, leftTable, rightTable);
                    if (whereValue.equalsIgnoreCase("IS NULL")) {
                        joinedRows = joinedRows.stream()
                                .filter(row -> row.get(whereIdx) == null)
                                .collect(Collectors.toList());
                    } else if (whereValue.equalsIgnoreCase("IS NOT NULL")) {
                        joinedRows = joinedRows.stream()
                                .filter(row -> row.get(whereIdx) != null)
                                .collect(Collectors.toList());
                    } else {
                        joinedRows = joinedRows.stream()
                                .filter(row -> {
                                    Object value = row.get(whereIdx);
                                    return value != null && value.toString().equalsIgnoreCase(whereValue);
                                })
                                .collect(Collectors.toList());
                    }
                }
                // ORDER BY with qualified column support
                if (orderByColumn != null) {
                    int orderIdx = getQualifiedColumnIndex(orderByColumn, leftTable, rightTable);
                    joinedRows = joinedRows.stream()
                            .sorted((a, b) -> {
                                Comparable va = (Comparable) a.get(orderIdx);
                                Comparable vb = (Comparable) b.get(orderIdx);
                                int cmp = 0;
                                if (va == null && vb == null) cmp = 0;
                                else if (va == null) cmp = -1;
                                else if (vb == null) cmp = 1;
                                else cmp = va.compareTo(vb);
                                return orderByAsc ? cmp : -cmp;
                            })
                            .collect(Collectors.toList());
                }
                if (offset != null && offset > 0) {
                    if (offset < joinedRows.size()) {
                        joinedRows = joinedRows.subList(offset, joinedRows.size());
                    } else {
                        joinedRows = List.of();
                    }
                }
                if (limit != null && limit >= 0 && limit < joinedRows.size()) {
                    joinedRows = joinedRows.subList(0, limit);
                }
                for (List<Object> row : joinedRows) {
                    // For joined rows, acquire read locks on both tuples if possible
                    // (Assume leftTuples and rightTuples are not mutated during SELECT)
                    // Not locking here as we only have List<Object>, but in a real system, track source tuples and lock both.
                    printJoinedRow(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error reading table: " + e.getMessage());
        }
    }

    // Helper: print header for joined tables
    private void printHeader(List<com.postgresql.catalog.ColumnMetadata> leftCols, List<com.postgresql.catalog.ColumnMetadata> rightCols) {
        StringBuilder sb = new StringBuilder();
        for (com.postgresql.catalog.ColumnMetadata col : leftCols) {
            sb.append(tableName).append(".").append(col.getName()).append(" | ");
        }
        if (rightCols != null) {
            for (com.postgresql.catalog.ColumnMetadata col : rightCols) {
                sb.append(joinTable).append(".").append(col.getName()).append(" | ");
            }
        }
        if (sb.length() > 3) sb.setLength(sb.length() - 3); // remove last ' | '
        System.out.println(sb.toString());
        System.out.println("-".repeat(sb.length()));
    }

    // Helper: print joined row
    private void printJoinedRow(List<Object> row) {
        String out = row.stream().map(val -> val == null ? "null" : val.toString()).collect(Collectors.joining(" | "));
        System.out.println(out);
    }

    // Helper: get column index for qualified or unqualified column name
    private int getQualifiedColumnIndex(String col, Table left, Table right) {
        // If qualified (table.column), use table name to resolve
        if (col.contains(".")) {
            String[] parts = col.split("\\.", 2);
            String table = parts[0];
            String column = parts[1];
            if (right == null || table.equalsIgnoreCase(left.getName())) {
                return left.getMetadata().getColumnIndex(column);
            } else if (right != null && table.equalsIgnoreCase(right.getName())) {
                return left.getMetadata().getColumns().size() + right.getMetadata().getColumnIndex(column);
            } else {
                throw new IllegalArgumentException("Unknown table in qualified column: " + col);
            }
        } else {
            // Unqualified: try left, then right (if present)
            try {
                return left.getMetadata().getColumnIndex(col);
            } catch (Exception ignore) {}
            if (right != null) {
                try {
                    return left.getMetadata().getColumns().size() + right.getMetadata().getColumnIndex(col);
                } catch (Exception ignore) {}
            }
            throw new IllegalArgumentException("Column not found: " + col);
        }
    }

    // Helper: sort, offset, limit for single-table SELECT
    private List<Tuple> sortOffsetLimit(List<Tuple> tuples, Table table, String orderByColumn, boolean orderByAsc, Integer limit, Integer offset) {
        if (orderByColumn != null) {
            int colIdx = table.getMetadata().getColumnIndex(orderByColumn);
            tuples = tuples.stream()
                    .sorted((a, b) -> {
                        Comparable va = (Comparable) a.getValues().get(colIdx);
                        Comparable vb = (Comparable) b.getValues().get(colIdx);
                        int cmp = 0;
                        if (va == null && vb == null) cmp = 0;
                        else if (va == null) cmp = -1;
                        else if (vb == null) cmp = 1;
                        else cmp = va.compareTo(vb);
                        return orderByAsc ? cmp : -cmp;
                    })
                    .collect(Collectors.toList());
        }
        if (offset != null && offset > 0) {
            if (offset < tuples.size()) {
                tuples = tuples.subList(offset, tuples.size());
            } else {
                tuples = List.of();
            }
        }
        if (limit != null && limit >= 0 && limit < tuples.size()) {
            tuples = tuples.subList(0, limit);
        }
        return tuples;
    }

    private void printRow(Tuple tuple) {
        String row = tuple.getValues().stream()
                .map(val -> val == null ? "null" : val.toString())
                .collect(Collectors.joining(" | "));
        System.out.println(row);
    }
}
