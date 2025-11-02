# PostgresLite - System Design Document

## Table of Contents
1. [High-Level Design (HLD)](#high-level-design-hld)
2. [Low-Level Design (LLD)](#low-level-design-lld)
3. [Design Patterns](#design-patterns)
4. [Data Flow & Algorithms](#data-flow--algorithms)
5. [Trade-offs and Design Decisions](#trade-offs-and-design-decisions)

---

# High-Level Design (HLD)

## 1. System Overview

### 1.1 Purpose
PostgresLite is a lightweight, educational implementation of a PostgreSQL-like relational database management system (RDBMS) built in Java. It provides a command-line interface for executing SQL-like queries with file-based persistent storage, demonstrating core database concepts and internals.

### 1.2 Key Requirements

#### Functional Requirements
- Execute SQL-like commands (CREATE, INSERT, SELECT, UPDATE, DELETE, ALTER)
- Support complex queries (JOIN, WHERE, ORDER BY, LIMIT, OFFSET)
- Persist data to disk using file-based storage
- Provide interactive command-line interface
- Manage table metadata (catalog management)
- Support multiple data types (INT, STRING, FLOAT, BOOLEAN)

#### Non-Functional Requirements
- **Maintainability**: Clean architecture with separation of concerns
- **Extensibility**: Easy to add new SQL commands
- **Educational**: Clear code demonstrating database internals
- **Testability**: Comprehensive test coverage
- **Simplicity**: Simple enough to understand, complex enough to be useful

### 1.3 Design Goals

- Demonstrate database architecture principles
- Implement fundamental SQL operations
- Show query parsing and execution
- Illustrate storage engine design
- Provide hands-on learning for database internals

---

## 2. System Architecture

### 2.1 High-Level Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                         User Layer                                 │
│                     (CLI Interface)                                │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ SQL Query String
                             │
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                            │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │           PostgresLiteShell.java                          │    │
│  │  - Main entry point                                       │    │
│  │  - Read-Eval-Print Loop (REPL)                           │    │
│  │  - User input handling                                    │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │           ConsoleUI.java                                  │    │
│  │  - Colored output formatting                              │    │
│  │  - Success/error messages                                 │    │
│  │  - ASCII art and UI elements                              │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ Raw SQL String
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                        Parser Layer                                │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │           CommandParser.java                              │    │
│  │                                                            │    │
│  │  - SQL string parsing                                     │    │
│  │  - Regex pattern matching                                 │    │
│  │  - Strategy pattern for different commands                │    │
│  │  - Validation and syntax checking                         │    │
│  │                                                            │    │
│  │  parserMap:                                               │    │
│  │    CREATE TABLE → createTableParser()                     │    │
│  │    INSERT INTO  → insertParser()                          │    │
│  │    SELECT       → selectParser()                          │    │
│  │    UPDATE       → updateParser()                          │    │
│  │    DELETE       → deleteParser()                          │    │
│  │    ALTER TABLE  → alterTableParser()                      │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ Command Object
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                      Command Layer                                 │
│                    (Command Pattern)                               │
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │ CreateTable    │  │ InsertCommand  │  │ SelectCommand  │     │
│  │ Command        │  │                │  │                │     │
│  └────────────────┘  └────────────────┘  └────────────────┘     │
│                                                                     │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │
│  │ UpdateCommand  │  │ DeleteCommand  │  │ AlterTable     │     │
│  │                │  │                │  │ Command        │     │
│  └────────────────┘  └────────────────┘  └────────────────┘     │
│                                                                     │
│  All implement: Command interface                                  │
│    - execute() method                                              │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ Catalog & Model Operations
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                    Catalog & Model Layer                           │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │        CatalogManager (Singleton)                         │    │
│  │  - Metadata management                                    │    │
│  │  - Table registry (Map<String, Table>)                   │    │
│  │  - Persistence coordination                               │    │
│  │  - Load/save operations                                   │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐   │
│  │ TableMetadata    │  │ ColumnMetadata   │  │ Table        │   │
│  │ - Table schema   │  │ - Column name    │  │ - Table data │   │
│  │ - Column list    │  │ - Data type      │  │ - Operations │   │
│  └──────────────────┘  └──────────────────┘  └──────────────┘   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │        Tuple                                              │    │
│  │  - Row representation                                     │    │
│  │  - List<Object> values                                   │    │
│  │  - Concurrency locks (ReentrantReadWriteLock)           │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ Data Access
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                      Storage Layer                                 │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │        TableHeap                                          │    │
│  │  - In-memory tuple storage                                │    │
│  │  - List<Tuple> tuples                                    │    │
│  │  - Insert, scan, delete operations                        │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │        TableSerializer                                    │    │
│  │  - File I/O operations                                    │    │
│  │  - Java serialization                                     │    │
│  │  - writeToDisk() / readFromDisk()                        │    │
│  └──────────────────────────────────────────────────────────┘    │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             │ Persistence
                             ▼
┌───────────────────────────────────────────────────────────────────┐
│                      File System Layer                             │
│                                                                     │
│  data/                                                             │
│  ├── users.table    (Metadata - TableMetadata serialized)         │
│  ├── users.tbl      (Data - Table with tuples serialized)         │
│  ├── orders.table   (Metadata)                                     │
│  └── orders.tbl     (Data)                                         │
│                                                                     │
│  File Format: Java ObjectOutputStream serialization               │
│  Extension: .table (metadata), .tbl (data)                         │
└───────────────────────────────────────────────────────────────────┘
```

### 2.2 Component Description

#### Presentation Layer
- **PostgresLiteShell**: REPL (Read-Eval-Print Loop) for user interaction
- **ConsoleUI**: Formatting utilities for colored output and user experience

#### Parser Layer
- **CommandParser**: Converts SQL strings to Command objects using regex and strategy pattern

#### Command Layer
- **Command Interface**: Base abstraction for all SQL operations
- **Concrete Commands**: CreateTableCommand, InsertCommand, SelectCommand, etc.
- Each command encapsulates execution logic for specific SQL operation

#### Catalog & Model Layer
- **CatalogManager**: Singleton managing all table metadata
- **TableMetadata**: Schema information (columns, types)
- **ColumnMetadata**: Column definitions
- **Table**: Represents a database table with data and operations
- **Tuple**: Represents a row with values and concurrency locks

#### Storage Layer
- **TableHeap**: In-memory storage of tuples
- **TableSerializer**: File I/O using Java serialization

---

## 3. Data Flow

### 3.1 Complete Query Execution Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ User Input: "SELECT * FROM users WHERE age > 18 ORDER BY name" │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│ Step 1: PostgresLiteShell receives input                        │
│ - Read line from console                                         │
│ - Trim whitespace                                                │
│ - Check for exit command                                         │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│ Step 2: CommandParser.parse(query)                              │
│ - Try each parser in parserMap                                   │
│ - Match "SELECT" keyword                                         │
│ - Extract: columns, table name, WHERE clause, ORDER BY          │
│ - Use regex patterns for parsing                                 │
│ - Create SelectCommand object                                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│ Step 3: SelectCommand.execute()                                 │
│ - Get table from CatalogManager                                  │
│ - Retrieve all tuples from TableHeap                             │
│ - Apply WHERE filter (age > 18)                                  │
│ - Apply ORDER BY (sort by name)                                  │
│ - Format and print results                                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│ Step 4: Display Results                                          │
│ - ConsoleUI formats output                                       │
│ - Print to console                                               │
│ - Return to REPL for next query                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 Table Creation Flow

```
User: CREATE TABLE users (id INT, name STRING, age INT)
                │
                ▼
┌───────────────────────────────────────────────────────────────┐
│ CommandParser.createTableParser()                             │
│ 1. Extract table name: "users"                                │
│ 2. Parse column definitions:                                  │
│    - "id INT" → ColumnMetadata(id, INT)                      │
│    - "name STRING" → ColumnMetadata(name, STRING)            │
│    - "age INT" → ColumnMetadata(age, INT)                    │
│ 3. Create CreateTableCommand                                  │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ CreateTableCommand.execute()                                  │
│ 1. Call CatalogManager.createTable(name, columns)            │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ CatalogManager.createTable()                                  │
│ 1. Check if table already exists (throw error if yes)         │
│ 2. Create TableMetadata(name, columns)                        │
│ 3. Create TableHeap() (empty)                                 │
│ 4. Create Table(name, metadata, heap)                         │
│ 5. Add to tables map                                          │
│ 6. Persist to disk via saveTable()                            │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ File System                                                    │
│ Creates:                                                       │
│ - data/users.table (serialized Table object)                  │
└───────────────────────────────────────────────────────────────┘
```

### 3.3 INSERT Flow

```
User: INSERT INTO users VALUES (1, 'Alice', 30)
                │
                ▼
┌───────────────────────────────────────────────────────────────┐
│ CommandParser.insertParser()                                  │
│ 1. Extract table name: "users"                                │
│ 2. Parse values: [1, 'Alice', 30]                            │
│ 3. Remove quotes from strings                                 │
│ 4. Create InsertCommand                                       │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ InsertCommand.execute()                                       │
│ 1. Get table from CatalogManager                              │
│ 2. Get column metadata                                        │
│ 3. Parse values according to column types:                    │
│    - "1" → Integer(1)                                         │
│    - "Alice" → String("Alice")                                │
│    - "30" → Integer(30)                                       │
│ 4. Create Tuple(values)                                       │
│ 5. Call table.insertTuple(tuple)                             │
│ 6. Persist to disk                                            │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ Table.insertTuple()                                           │
│ → TableHeap.insertTuple()                                     │
│   → tuples.add(tuple)                                         │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ CatalogManager.saveTable()                                    │
│ → Serialize entire table to data/users.table                  │
└───────────────────────────────────────────────────────────────┘
```

### 3.4 SELECT with JOIN Flow

```
User: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id
                │
                ▼
┌───────────────────────────────────────────────────────────────┐
│ CommandParser.selectParser()                                  │
│ 1. Detect "INNER JOIN" keyword                                │
│ 2. Extract:                                                    │
│    - Left table: "users"                                       │
│    - Right table: "orders"                                     │
│    - Join condition: "users.id = orders.user_id"              │
│ 3. Parse qualified column names                               │
│ 4. Create SelectCommand with join info                        │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌───────────────────────────────────────────────────────────────┐
│ SelectCommand.execute() - JOIN Algorithm                      │
│                                                                │
│ 1. Get both tables from CatalogManager                         │
│ 2. Parse join condition:                                       │
│    - Left column: users.id (index in users table)             │
│    - Right column: orders.user_id (index in orders table)     │
│                                                                │
│ 3. Nested Loop Join Algorithm:                                │
│    FOR EACH leftTuple IN users.getAllTuples():                │
│      FOR EACH rightTuple IN orders.getAllTuples():            │
│        IF leftTuple[id] == rightTuple[user_id]:               │
│          Create joined tuple [left values + right values]     │
│          Add to results                                        │
│                                                                │
│ 4. Apply WHERE filter (if present)                            │
│ 5. Apply ORDER BY (if present)                                │
│ 6. Apply LIMIT/OFFSET (if present)                            │
│ 7. Format and print results                                   │
└───────────────────────────────────────────────────────────────┘
```

---

## 4. Database Schema Design

### 4.1 Metadata Representation

#### TableMetadata Structure
```java
class TableMetadata {
    String tableName;
    List<ColumnMetadata> columns;
    Map<String, Integer> columnIndexMap;  // Fast column lookup
    Map<String, ColumnMetadata> columnMetadataMap;
}
```

#### ColumnMetadata Structure
```java
class ColumnMetadata {
    String columnName;
    DataType type;  // INT, STRING, FLOAT, BOOLEAN
}
```

### 4.2 Data Storage Structure

#### In-Memory Representation
```
Table "users":
┌─────────────────────────────────────────────────────┐
│ TableMetadata                                        │
│ - tableName: "users"                                 │
│ - columns: [id:INT, name:STRING, age:INT]           │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│ TableHeap                                            │
│ - tuples: List<Tuple>                               │
│   [                                                  │
│     Tuple([1, "Alice", 30]),                        │
│     Tuple([2, "Bob", 25]),                          │
│     Tuple([3, "Charlie", 35])                       │
│   ]                                                  │
└─────────────────────────────────────────────────────┘
```

#### On-Disk Representation
```
data/users.table:
┌──────────────────────────────────────────┐
│ Serialized Table Object                  │
│ - TableMetadata                           │
│ - TableHeap with all tuples              │
│ - Complete object graph                   │
└──────────────────────────────────────────┘

File format: Java ObjectOutputStream
Serialization: Native Java serialization
```

---

## 5. Query Processing

### 5.1 SQL Parsing Strategy

PostgresLite uses a **Strategy Pattern** for parsing different SQL commands:

```java
Map<String, Function<String, Command>> parserMap = new LinkedHashMap<>();

Static initialization:
parserMap.put("CREATE TABLE", this::createTableParser);
parserMap.put("INSERT INTO", this::insertParser);
parserMap.put("SELECT", this::selectParser);
parserMap.put("UPDATE", this::updateParser);
parserMap.put("DELETE FROM", this::deleteParser);
parserMap.put("ALTER TABLE", this::alterTableParser);
```

#### Parsing Algorithm
```
1. Iterate through parserMap (ordered)
2. Check if query starts with keyword
3. If match found:
   a. Call corresponding parser function
   b. Return Command object
4. If no match:
   a. Throw InvalidSyntaxException
```

### 5.2 SELECT Query Processing Pipeline

```
Raw Query
    │
    ▼
┌─────────────────┐
│ Parse SELECT    │ Extract columns, table, clauses
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Get Table       │ CatalogManager.getTable(name)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Scan Tuples     │ tableHeap.scanAllTuples()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Apply WHERE     │ Filter using Stream.filter()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Apply ORDER BY  │ Sort using Stream.sorted()
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Apply LIMIT     │ List.subList(offset, limit)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Format Output   │ ConsoleUI.printResult()
└─────────────────┘
```

### 5.3 JOIN Processing Algorithm

#### INNER JOIN (Nested Loop)
```java
List<Tuple> results = new ArrayList<>();

for (Tuple leftTuple : leftTable.getAllTuples()) {
    for (Tuple rightTuple : rightTable.getAllTuples()) {
        // Check join condition
        Object leftValue = leftTuple.getValues().get(leftJoinColumnIndex);
        Object rightValue = rightTuple.getValues().get(rightJoinColumnIndex);

        if (Objects.equals(leftValue, rightValue)) {
            // Merge tuples
            List<Object> merged = new ArrayList<>();
            merged.addAll(leftTuple.getValues());
            merged.addAll(rightTuple.getValues());
            results.add(new Tuple(merged));
        }
    }
}
```

**Time Complexity**: O(n × m) where n, m are table sizes

#### LEFT JOIN
```java
for (Tuple leftTuple : leftTable.getAllTuples()) {
    boolean matched = false;

    for (Tuple rightTuple : rightTable.getAllTuples()) {
        if (joinConditionMatches(leftTuple, rightTuple)) {
            results.add(merge(leftTuple, rightTuple));
            matched = true;
        }
    }

    // LEFT JOIN: Include left tuple even if no match
    if (!matched) {
        results.add(merge(leftTuple, createNullTuple(rightColumnCount)));
    }
}
```

#### RIGHT JOIN
```java
// Similar to LEFT JOIN, but iterate right table first
// Include right tuples even if no match (with NULLs for left)
```

---

## 6. Concurrency Control

### 6.1 Tuple-Level Locking

```java
class Tuple {
    private List<Object> values;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void acquireReadLock() {
        lock.readLock().lock();
    }

    public void releaseReadLock() {
        lock.readLock().unlock();
    }
}
```

### 6.2 Usage in SELECT Query

```java
try {
    for (Tuple tuple : tuples) {
        tuple.acquireReadLock();
        // Read tuple values
    }
} finally {
    for (Tuple tuple : tuples) {
        tuple.releaseReadLock();
    }
}
```

### 6.3 Concurrency Model

- **Read Locks**: Multiple threads can read simultaneously
- **Write Locks**: Exclusive (not currently implemented fully)
- **Lock Granularity**: Tuple-level (row-level)
- **Deadlock Prevention**: Try-finally ensures lock release

---

# Low-Level Design (LLD)

## 1. Class Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                     Command Pattern Layer                       │
└────────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │   «interface»    │
                    │    Command       │
                    ├──────────────────┤
                    │ + execute(): void│
                    └────────▲─────────┘
                             │
                    ┌────────┴────────────────────────┐
                    │                                 │
        ┌───────────┴──────────┐          ┌──────────┴──────────┐
        │ CreateTableCommand   │          │  InsertCommand      │
        ├──────────────────────┤          ├─────────────────────┤
        │ - tableName: String  │          │ - tableName: String │
        │ - columns: List      │          │ - values: List      │
        ├──────────────────────┤          ├─────────────────────┤
        │ + execute(): void    │          │ + execute(): void   │
        └──────────────────────┘          └─────────────────────┘

        ┌──────────────────────┐          ┌─────────────────────┐
        │  SelectCommand       │          │  UpdateCommand      │
        ├──────────────────────┤          ├─────────────────────┤
        │ - tableName: String  │          │ - tableName: String │
        │ - columns: List      │          │ - setColumn: String │
        │ - whereColumn: String│          │ - setValue: String  │
        │ - joinType: String   │          │ - whereColumn       │
        │ - orderBy: String    │          ├─────────────────────┤
        │ - limit: int         │          │ + execute(): void   │
        ├──────────────────────┤          └─────────────────────┘
        │ + execute(): void    │
        └──────────────────────┘          ┌─────────────────────┐
                                          │  DeleteCommand      │
        ┌──────────────────────┐          ├─────────────────────┤
        │ AlterTableCommand    │          │ - tableName: String │
        ├──────────────────────┤          │ - whereColumn       │
        │ - tableName: String  │          │ - whereValue        │
        │ - newColumn          │          ├─────────────────────┤
        ├──────────────────────┤          │ + execute(): void   │
        │ + execute(): void    │          └─────────────────────┘
        └──────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                      Catalog & Model Layer                      │
└────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              CatalogManager (Singleton)                      │
├─────────────────────────────────────────────────────────────┤
│ - INSTANCE: CatalogManager (static)                         │
│ - tables: Map<String, Table>                                │
│ - DB_PATH: String = "./data/"                               │
├─────────────────────────────────────────────────────────────┤
│ + getInstance(): CatalogManager (static)                    │
│ + createTable(name, columns): void                          │
│ + getTable(name): Table                                     │
│ + listTables(): List<String>                                │
│ + saveTable(table): void                                    │
│ + addColumn(tableName, column): void                        │
│ - loadTablesFromDisk(): void                                │
└─────────────────────────┬───────────────────────────────────┘
                          │ manages
                          ▼
              ┌───────────────────────┐
              │      Table            │
              ├───────────────────────┤
              │ - name: String        │
              │ - metadata: TableMeta │
              │ - tableHeap: TableHeap│
              ├───────────────────────┤
              │ + insertTuple(tuple)  │
              │ + getAllTuples(): List│
              │ + updateTuples(...)   │
              │ + deleteTuples(...)   │
              └───────┬───────────────┘
                      │
         ┌────────────┴─────────────┐
         │                          │
         ▼                          ▼
┌────────────────┐        ┌───────────────────┐
│ TableMetadata  │        │   TableHeap       │
├────────────────┤        ├───────────────────┤
│ - tableName    │        │ - tuples: List    │
│ - columns: List│        ├───────────────────┤
│ - columnIndexMap│       │ + insertTuple()   │
│ - columnMetaMap│        │ + scanAllTuples() │
├────────────────┤        │ + deleteTuples()  │
│ + getColumnIdx()│       └───────────────────┘
│ + getColumnByName()│              │
│ + addColumn()  │                  │ contains
└────────┬───────┘                  ▼
         │                 ┌─────────────────┐
         │ has             │     Tuple       │
         ▼                 ├─────────────────┤
┌────────────────┐         │ - values: List  │
│ColumnMetadata │         │ - lock: RRWLock │
├────────────────┤         ├─────────────────┤
│ - columnName   │         │ + getValues()   │
│ - type: DataType│        │ + acquireReadLock()│
├────────────────┤         │ + releaseReadLock()│
│ + getName()    │         └─────────────────┘
│ + getType()    │
└────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                       Storage Layer                             │
└────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│        TableSerializer                    │
├──────────────────────────────────────────┤
│ - DATA_DIR: String = "data"              │
├──────────────────────────────────────────┤
│ + writeToDisk(table): void (static)      │
│ + readFromDisk(tableName): Table (static)│
└──────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                        Parser Layer                             │
└────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              CommandParser                                   │
├─────────────────────────────────────────────────────────────┤
│ - parserMap: LinkedHashMap<String, Function>               │
├─────────────────────────────────────────────────────────────┤
│ + parse(query): Command                                     │
│ - createTableParser(query): Command                         │
│ - insertParser(query): Command                              │
│ - selectParser(query): Command                              │
│ - updateParser(query): Command                              │
│ - deleteParser(query): Command                              │
│ - alterTableParser(query): Command                          │
└─────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     Common/Utility Layer                        │
└────────────────────────────────────────────────────────────────┘

┌──────────────────┐          ┌────────────────────────┐
│   DataType       │          │   CommandConstants     │
│   «enumeration»  │          ├────────────────────────┤
├──────────────────┤          │ + CREATE_TABLE: String │
│ INT              │          │ + INSERT_INTO: String  │
│ STRING           │          │ + SELECT: String       │
│ BOOLEAN          │          │ + UPDATE: String       │
│ FLOAT            │          │ + DELETE_FROM: String  │
└──────────────────┘          └────────────────────────┘
```

---

## 2. Sequence Diagrams

### 2.1 CREATE TABLE Sequence

```
User    Shell    Parser    CreateTable   Catalog    Table    TableSerializer    FileSystem
 │        │         │       Command        Manager   Metadata                       │
 │ CREATE │         │           │             │         │           │               │
 │ TABLE  │         │           │             │         │           │               │
 ├───────>│         │           │             │         │           │               │
 │        │ parse() │           │             │         │           │               │
 │        ├────────>│           │             │         │           │               │
 │        │         │ new       │             │         │           │               │
 │        │         ├──────────>│             │         │           │               │
 │        │  cmd    │           │             │         │           │               │
 │        │<────────┤           │             │         │           │               │
 │        │         │           │             │         │           │               │
 │        │ execute()│          │             │         │           │               │
 │        ├──────────────────────>            │         │           │               │
 │        │         │           │createTable()│         │           │               │
 │        │         │           ├────────────>│         │           │               │
 │        │         │           │             │ new     │           │               │
 │        │         │           │             ├────────>│           │               │
 │        │         │           │             │         │ new Table │               │
 │        │         │           │             ├──────────────────────>              │
 │        │         │           │             │saveTable()          │               │
 │        │         │           │             ├────────────────────>│               │
 │        │         │           │             │         │           │ write         │
 │        │         │           │             │         │           ├──────────────>│
 │        │         │           │             │         │           │  .table       │
 │        │         │           │             │         │           │<──────────────┤
 │        │         │           │             │         │           │  OK           │
 │   ✅   │         │           │             │         │           │               │
 │<───────┤         │           │             │         │           │               │
```

### 2.2 INSERT Sequence

```
User    Shell    Parser   InsertCmd   Catalog   Table   TableHeap   Serializer   FileSystem
 │        │         │         │          │        │         │           │           │
 │ INSERT │         │         │          │        │         │           │           │
 ├───────>│         │         │          │        │         │           │           │
 │        │ parse() │         │          │        │         │           │           │
 │        ├────────>│         │          │        │         │           │           │
 │        │         │ new     │          │        │         │           │           │
 │        │         ├────────>│          │        │         │           │           │
 │        │  cmd    │         │          │        │         │           │           │
 │        │<────────┤         │          │        │         │           │           │
 │        │         │         │          │        │         │           │           │
 │        │ execute()│        │          │        │         │           │           │
 │        ├─────────────────────>         │        │         │           │           │
 │        │         │         │getTable() │        │         │           │           │
 │        │         │         ├──────────>│        │         │           │           │
 │        │         │         │   table   │        │         │           │           │
 │        │         │         │<──────────┤        │         │           │           │
 │        │         │         │           │        │         │           │           │
 │        │         │         │  insertTuple()     │         │           │           │
 │        │         │         ├───────────────────>│         │           │           │
 │        │         │         │           │        │insert() │           │           │
 │        │         │         │           │        ├────────>│           │           │
 │        │         │         │           │        │  OK     │           │           │
 │        │         │         │           │        │<────────┤           │           │
 │        │         │         │           │        │         │           │           │
 │        │         │         │  saveTable()       │         │           │           │
 │        │         │         ├───────────────────────────────────────────>          │
 │        │         │         │           │        │         │           │ write     │
 │        │         │         │           │        │         │           ├──────────>│
 │        │         │         │           │        │         │           │  .table   │
 │        │         │         │           │        │         │           │<──────────┤
 │   ✅   │         │         │           │        │         │           │           │
 │<───────┤         │         │           │        │         │           │           │
```

### 2.3 SELECT with WHERE and ORDER BY Sequence

```
User    Shell    Parser   SelectCmd   Catalog   Table   TableHeap   ConsoleUI
 │        │         │         │          │        │         │           │
 │ SELECT │         │         │          │        │         │           │
 │ WHERE  │         │         │          │        │         │           │
 │ ORDER BY│        │         │          │        │         │           │
 ├───────>│         │         │          │        │         │           │
 │        │ parse() │         │          │        │         │           │
 │        ├────────>│         │          │        │         │           │
 │        │         │         │          │        │         │           │
 │        │         │ Extract: tableName, columns, WHERE, ORDER BY      │
 │        │         │         │          │        │         │           │
 │        │         │ new     │          │        │         │           │
 │        │         ├────────>│          │        │         │           │
 │        │  cmd    │         │          │        │         │           │
 │        │<────────┤         │          │        │         │           │
 │        │         │         │          │        │         │           │
 │        │ execute()│        │          │        │         │           │
 │        ├─────────────────────>         │        │         │           │
 │        │         │         │getTable() │        │         │           │
 │        │         │         ├──────────>│        │         │           │
 │        │         │         │   table   │        │         │           │
 │        │         │         │<──────────┤        │         │           │
 │        │         │         │           │        │         │           │
 │        │         │         │  getAllTuples()    │         │           │
 │        │         │         ├───────────────────>│         │           │
 │        │         │         │           │        │scanAll()│           │
 │        │         │         │           │        ├────────>│           │
 │        │         │         │  tuples   │        │  list   │           │
 │        │         │         │<───────────────────┤<────────┤           │
 │        │         │         │           │        │         │           │
 │        │         │         │ Apply WHERE filter (Stream.filter())     │
 │        │         │         │ Apply ORDER BY (Stream.sorted())         │
 │        │         │         │ Apply LIMIT/OFFSET (subList())           │
 │        │         │         │           │        │         │           │
 │        │         │         │           │        │         │ print()   │
 │        │         │         ├───────────────────────────────────────────>
 │        │         │         │           │        │         │           │
 │ Results│         │         │           │        │         │           │
 │<───────────────────────────────────────────────────────────────────────┤
```

### 2.4 JOIN Sequence (INNER JOIN)

```
User    Shell    Parser   SelectCmd   Catalog   LeftTable  RightTable  ConsoleUI
 │        │         │         │          │          │           │           │
 │ SELECT │         │         │          │          │           │           │
 │ INNER  │         │         │          │          │           │           │
 │ JOIN   │         │         │          │          │           │           │
 ├───────>│         │         │          │          │           │           │
 │        │ parse() │         │          │          │           │           │
 │        ├────────>│         │          │          │           │           │
 │        │         │         │          │          │           │           │
 │        │         │ Extract JOIN info (tables, condition)                │
 │        │         │         │          │          │           │           │
 │        │         │ new     │          │          │           │           │
 │        │         ├────────>│          │          │           │           │
 │        │  cmd    │         │          │          │           │           │
 │        │<────────┤         │          │          │           │           │
 │        │         │         │          │          │           │           │
 │        │ execute()│        │          │          │           │           │
 │        ├─────────────────────>         │          │           │           │
 │        │         │         │getTable(left)       │           │           │
 │        │         │         ├──────────>│          │           │           │
 │        │         │         │ leftTable │          │           │           │
 │        │         │         │<──────────┤          │           │           │
 │        │         │         │           │          │           │           │
 │        │         │         │getTable(right)       │           │           │
 │        │         │         ├──────────────────────────────────>          │
 │        │         │         │           │          │ rightTable│           │
 │        │         │         │<──────────────────────────────────┤          │
 │        │         │         │           │          │           │           │
 │        │         │         │ getAllTuples() (left)│           │           │
 │        │         │         ├──────────────────────>           │           │
 │        │         │         │ leftTuples│          │           │           │
 │        │         │         │<──────────────────────┤           │           │
 │        │         │         │           │          │           │           │
 │        │         │         │ getAllTuples() (right)           │           │
 │        │         │         ├──────────────────────────────────>          │
 │        │         │         │           │          │ rightTuples          │
 │        │         │         │<──────────────────────────────────┤          │
 │        │         │         │           │          │           │           │
 │        │         │         │ Nested Loop Join Algorithm:                 │
 │        │         │         │   FOR leftTuple IN leftTuples:              │
 │        │         │         │     FOR rightTuple IN rightTuples:          │
 │        │         │         │       IF joinCondition(left, right):        │
 │        │         │         │         merge(left, right) → result         │
 │        │         │         │           │          │           │           │
 │        │         │         │ Apply WHERE, ORDER BY, LIMIT                │
 │        │         │         │           │          │           │           │
 │        │         │         │           │          │           │ print()   │
 │        │         │         ├───────────────────────────────────────────────>
 │        │         │         │           │          │           │           │
 │ Results│         │         │           │          │           │           │
 │<───────────────────────────────────────────────────────────────────────────┤
```

---

## 3. Component Detailed Design

### 3.1 CommandParser Implementation

```java
public class CommandParser {
    private final Map<String, Function<String, Command>> parserMap;

    public CommandParser() {
        // LinkedHashMap preserves insertion order
        parserMap = new LinkedHashMap<>();

        // Order matters: More specific patterns first
        parserMap.put("CREATE TABLE", this::createTableParser);
        parserMap.put("INSERT INTO", this::insertParser);
        parserMap.put("SELECT", this::selectParser);
        parserMap.put("UPDATE", this::updateParser);
        parserMap.put("DELETE FROM", this::deleteParser);
        parserMap.put("ALTER TABLE", this::alterTableParser);
    }

    public Command parse(String query) {
        String normalized = query.trim().toUpperCase();

        for (Map.Entry<String, Function<String, Command>> entry : parserMap.entrySet()) {
            if (normalized.startsWith(entry.getKey())) {
                return entry.getValue().apply(query);
            }
        }

        throw new InvalidSyntaxException("Unknown command: " + query);
    }
}
```

**Time Complexity**: O(k) where k = number of command types (constant)

---

### 3.2 CatalogManager (Singleton Pattern)

```java
public class CatalogManager {
    // Thread-safe singleton using eager initialization
    private static final CatalogManager INSTANCE = new CatalogManager();

    private final Map<String, Table> tables = new HashMap<>();
    private static final String DB_PATH = "./data/";

    // Private constructor prevents external instantiation
    private CatalogManager() {
        loadTablesFromDisk();
    }

    // Global access point
    public static CatalogManager getInstance() {
        return INSTANCE;
    }

    // Table management
    public void createTable(String name, List<ColumnMetadata> columns) {
        if (tables.containsKey(name)) {
            throw new RuntimeException("Table already exists: " + name);
        }

        TableMetadata metadata = new TableMetadata(name, columns);
        TableHeap heap = new TableHeap();
        Table table = new Table(name, metadata, heap);

        tables.put(name, table);
        saveTable(table);
    }
}
```

**Design Benefits**:
- Single source of truth for metadata
- Thread-safe (eager initialization)
- Global access point
- Prevents multiple catalog instances

---

### 3.3 Table and Tuple Design

```java
public class Table implements Serializable {
    private final String name;
    private final TableMetadata metadata;
    private final TableHeap tableHeap;

    public void insertTuple(Tuple tuple) {
        // Validate tuple matches schema
        if (tuple.getValues().size() != metadata.getColumns().size()) {
            throw new RuntimeException("Tuple size mismatch");
        }

        tableHeap.insertTuple(tuple);
    }

    public int updateTuples(String targetCol, String newVal,
                            String whereCol, String whereVal) {
        int targetIdx = metadata.getColumnIndex(targetCol);
        int whereIdx = metadata.getColumnIndex(whereCol);

        Object parsedWhere = parseValue(whereCol, whereVal);
        Object parsedNew = parseValue(targetCol, newVal);

        int updated = 0;
        for (Tuple tuple : tableHeap.scanAllTuples()) {
            Object actual = tuple.getValues().get(whereIdx);
            if (Objects.equals(actual, parsedWhere)) {
                tuple.getValues().set(targetIdx, parsedNew);
                updated++;
            }
        }

        return updated;
    }
}
```

```java
public class Tuple implements Serializable {
    private final List<Object> values;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void acquireReadLock() {
        lock.readLock().lock();
    }

    public void releaseReadLock() {
        lock.readLock().unlock();
    }

    public List<Object> getValues() {
        return values;
    }
}
```

---

### 3.4 Storage Layer: TableSerializer

```java
public class TableSerializer {
    private static final String DATA_DIR = "data";

    public static void writeToDisk(Table table) throws IOException {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = DATA_DIR + "/" +
                         table.getMetadata().getTableName() + ".tbl";

        try (ObjectOutputStream oos =
             new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(table);
        }
    }

    public static Table readFromDisk(String tableName)
                        throws IOException, ClassNotFoundException {
        String fileName = DATA_DIR + "/" + tableName + ".tbl";
        File file = new File(fileName);

        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois =
             new ObjectInputStream(new FileInputStream(file))) {
            return (Table) ois.readObject();
        }
    }
}
```

**Serialization Details**:
- Uses Java's native serialization
- Entire object graph serialized (Table → TableMetadata → TableHeap → Tuples)
- Trade-off: Simple but not space-efficient or version-tolerant

---

## 4. Algorithm Analysis

### 4.1 Time Complexity Summary

| Operation | Current Implementation | With Indexing |
|-----------|------------------------|---------------|
| INSERT | O(1) + O(n) serialize | O(log n) |
| SELECT (no WHERE) | O(n) | O(n) |
| SELECT (with WHERE) | O(n) | O(log n) |
| UPDATE | O(n) | O(log n) |
| DELETE | O(n) | O(log n) |
| INNER JOIN | O(n × m) | O(n + m) with hash join |
| LEFT/RIGHT JOIN | O(n × m) | O(n + m) with hash join |
| ORDER BY | O(n log n) | O(n log n) |

Where:
- n = number of tuples in table
- m = number of tuples in joined table

### 4.2 Space Complexity

| Component | Space Complexity |
|-----------|------------------|
| Tuple storage | O(n × c) where c = avg tuple size |
| Catalog metadata | O(t × col) where t = tables, col = columns |
| Join result | O(n × m) worst case |
| Sorted result | O(n) additional space |

---

# Design Patterns

## 1. Command Pattern

**Intent**: Encapsulate SQL operations as objects

**Implementation**:
```
Command (interface)
    ├── CreateTableCommand
    ├── InsertCommand
    ├── SelectCommand
    ├── UpdateCommand
    ├── DeleteCommand
    └── AlterTableCommand
```

**Benefits**:
- **Extensibility**: Easy to add new commands
- **Encapsulation**: Each command knows how to execute itself
- **Separation**: UI/Parser separated from execution logic
- **Testability**: Each command can be tested independently

**Example**:
```java
Command cmd = parser.parse("SELECT * FROM users");
cmd.execute();  // Polymorphic execution
```

---

## 2. Singleton Pattern

**Intent**: Ensure single instance of CatalogManager

**Implementation**:
```java
public class CatalogManager {
    private static final CatalogManager INSTANCE = new CatalogManager();

    private CatalogManager() {
        // Private constructor
    }

    public static CatalogManager getInstance() {
        return INSTANCE;
    }
}
```

**Benefits**:
- **Single source of truth**: One catalog for all metadata
- **Thread-safe**: Eager initialization
- **Global access**: Available throughout application
- **Prevents inconsistency**: Can't create multiple catalogs

---

## 3. Strategy Pattern

**Intent**: Different parsing strategies for different SQL commands

**Implementation**:
```java
Map<String, Function<String, Command>> parserMap = new LinkedHashMap<>();
parserMap.put("CREATE TABLE", this::createTableParser);
parserMap.put("SELECT", this::selectParser);
// etc.
```

**Benefits**:
- **Flexibility**: Easy to add new parsers
- **Maintainability**: Each parser is independent
- **Open/Closed**: Open for extension, closed for modification

---

## 4. Repository Pattern

**Intent**: Abstract data persistence

**Implementation**:
```
TableSerializer (Repository)
    ├── writeToDisk()
    └── readFromDisk()
```

**Benefits**:
- **Separation of concerns**: Storage logic separate from business logic
- **Testability**: Can mock serializer for testing
- **Flexibility**: Easy to swap storage implementation (JSON, binary, database)

---

# Data Flow & Algorithms

## 1. Query Execution Pipeline

### 1.1 SELECT Query Execution

```
Input: "SELECT name, age FROM users WHERE age > 18 ORDER BY name LIMIT 10"

Step 1: Parse Query
├─> Extract table: "users"
├─> Extract columns: ["name", "age"]
├─> Extract WHERE: age > 18
├─> Extract ORDER BY: name ASC
└─> Extract LIMIT: 10

Step 2: Get Table
└─> CatalogManager.getTable("users")

Step 3: Scan Tuples
└─> table.getAllTuples() → List<Tuple>

Step 4: Apply WHERE Filter
└─> Stream.filter(tuple -> tuple.get(ageIdx) > 18)

Step 5: Apply ORDER BY
└─> Stream.sorted(Comparator.comparing(tuple -> tuple.get(nameIdx)))

Step 6: Apply LIMIT
└─> Stream.limit(10).collect()

Step 7: Format Output
└─> Extract requested columns (name, age)
    └─> Print to console
```

**Time Complexity**: O(n log n) due to sorting

### 1.2 JOIN Algorithm (Nested Loop)

```
Input: SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id

Nested Loop Join:
FOR EACH leftTuple IN users:
    FOR EACH rightTuple IN orders:
        IF leftTuple.id == rightTuple.user_id:
            result.add(merge(leftTuple, rightTuple))

RETURN result
```

**Time Complexity**: O(n × m)

**Optimization Opportunities**:
- Hash Join: O(n + m)
- Merge Join: O(n log n + m log m)
- Index Join: O(n × log m) if index on join column

---

## 2. Storage Engine

### 2.1 File Format

```
Table Serialization:

┌──────────────────────────────────────┐
│ Java Object Serialization Format     │
├──────────────────────────────────────┤
│ Header: Class metadata               │
│ - Class name                          │
│ - serialVersionUID                    │
│ - Field descriptors                   │
├──────────────────────────────────────┤
│ Data: Object fields                   │
│ - Table.name (String)                │
│ - Table.metadata (TableMetadata)     │
│   - tableName                         │
│   - columns (List<ColumnMetadata>)   │
│ - Table.tableHeap (TableHeap)        │
│   - tuples (List<Tuple>)             │
│     - values (List<Object>)          │
└──────────────────────────────────────┘
```

### 2.2 Load on Startup

```
Application Startup:
1. CatalogManager constructor called
2. loadTablesFromDisk() invoked
3. Scan data/ directory for .table files
4. For each file:
   - Deserialize Table object
   - Add to tables map
5. Ready to execute queries
```

---

# Trade-offs and Design Decisions

## 1. Java Serialization vs Custom Format

### Decision: Java Serialization

**Alternatives Considered**:
1. Java native serialization ✓
2. JSON format
3. Custom binary format
4. CSV files

**Comparison**:

| Aspect | Java Serialization | JSON | Custom Binary | CSV |
|--------|-------------------|------|---------------|-----|
| Implementation | Easy | Medium | Hard | Easy |
| Size | Large | Medium | Small | Medium |
| Type safety | Yes | Partial | Yes | No |
| Human readable | No | Yes | No | Yes |
| Version tolerance | Poor | Good | Good | Poor |
| Performance | Medium | Slow | Fast | Medium |

**Trade-offs**:
- ✓ Simple to implement
- ✓ Type-safe
- ✓ Handles complex objects
- ❌ Not space-efficient
- ❌ Not version-tolerant
- ❌ Not human-readable

**Decision Justification**: Educational project prioritizes simplicity. Java serialization requires minimal code and handles object graphs automatically.

---

## 2. Nested Loop Join vs Hash Join

### Decision: Nested Loop Join

**Alternatives Considered**:
1. Nested loop join ✓
2. Hash join
3. Merge join

**Comparison**:

| Join Algorithm | Time | Space | Implementation |
|---------------|------|-------|----------------|
| Nested Loop | O(n×m) | O(1) | Simple |
| Hash Join | O(n+m) | O(n) | Medium |
| Merge Join | O(n log n + m log m) | O(1) | Medium |

**Trade-offs**:
- ✓ Simplest to implement
- ✓ No additional data structures
- ✓ Works for all join types
- ❌ Poor performance for large tables
- ❌ Not optimized

**Decision Justification**: Educational focus. Nested loop is easiest to understand. For small datasets (typical in learning context), performance difference is negligible.

---

## 3. Catalog Manager Singleton vs Dependency Injection

### Decision: Singleton Pattern

**Trade-offs**:
- ✓ Simple global access
- ✓ Guaranteed single instance
- ✓ No dependency injection framework needed
- ❌ Global state (testing challenges)
- ❌ Tight coupling
- ❌ Harder to mock

**Decision Justification**: Single-user, single-process application. No need for multiple catalog instances. Singleton simplifies access.

---

## 4. In-Memory Storage vs Buffer Pool

### Decision: Full In-Memory Storage

**Alternatives Considered**:
1. Load entire table to memory ✓
2. Buffer pool with pages
3. Memory-mapped files

**Trade-offs**:
- ✓ Simple implementation
- ✓ Fast access (no I/O during queries)
- ❌ Memory intensive for large tables
- ❌ No memory management
- ❌ Limited scalability

**Decision Justification**: Educational project with small datasets. Real databases use buffer pools for memory management.

---

## 5. Tuple-Level Locking vs Table-Level

### Decision: Tuple-Level Locking

**Trade-offs**:
- ✓ Fine-grained concurrency
- ✓ Better parallelism
- ❌ More complex
- ❌ Higher overhead

**Decision Justification**: Demonstrates row-level locking (like real databases). Shows understanding of concurrency granularity.

---

## 6. Immediate Persistence vs Write-Ahead Log

### Decision: Immediate Persistence

**Alternatives Considered**:
1. Write to disk after each modification ✓
2. Write-ahead logging (WAL)
3. Periodic checkpoints

**Trade-offs**:
- ✓ Simple durability guarantee
- ✓ No need for recovery mechanism
- ❌ Poor write performance
- ❌ No transaction support

**Decision Justification**: Simplicity for educational project. Real databases use WAL for better performance and ACID properties.

---

# Future Enhancements

## 1. Indexing Support

### B-Tree Index Design

```
Index Structure:
┌────────────────────────────────────┐
│        Index Manager               │
├────────────────────────────────────┤
│ Map<String, Map<String, Index>>   │
│   TableName → ColumnName → Index   │
└────────────────────────────────────┘

B-Tree Node:
┌────────────────────────────────────┐
│ keys: [10, 20, 30]                │
│ values: [Tuple1, Tuple2, Tuple3]  │
│ children: [Node, Node, Node, Node]│
└────────────────────────────────────┘
```

**Implementation Steps**:
1. Add `CREATE INDEX` command
2. Build B-tree on indexed column
3. Modify SELECT to use index for WHERE clauses
4. Update index on INSERT/UPDATE/DELETE

**Performance Gain**: O(n) → O(log n) for indexed queries

---

## 2. Transaction Support

### ACID Transaction Design

```
Transaction Manager:
┌────────────────────────────────────┐
│ - activeTransactions: Map          │
│ - writeAheadLog: WAL               │
├────────────────────────────────────┤
│ + begin(): TransactionID           │
│ + commit(txnID): void              │
│ + rollback(txnID): void            │
└────────────────────────────────────┘

Write-Ahead Log:
┌────────────────────────────────────┐
│ [TXN1] BEGIN                       │
│ [TXN1] UPDATE users SET age=31 ... │
│ [TXN1] INSERT INTO orders ...      │
│ [TXN1] COMMIT                      │
└────────────────────────────────────┘
```

**Implementation Steps**:
1. Add BEGIN, COMMIT, ROLLBACK commands
2. Implement WAL for durability
3. Add undo/redo logging
4. Implement recovery mechanism

---

## 3. Query Optimization

### Cost-Based Optimizer

```
Query Plan:
SELECT * FROM users
JOIN orders ON users.id = orders.user_id
WHERE users.age > 18

Unoptimized Plan:
1. Scan users (1M rows)
2. Scan orders (10M rows)
3. Join (10M × 1M comparisons)
4. Filter age > 18

Optimized Plan:
1. Scan users WHERE age > 18 (100K rows after filter)
2. Scan orders (10M rows)
3. Hash join (100K build, 10M probe)

Cost: 10M comparisons → 10.1M operations (100x faster)
```

**Implementation Steps**:
1. Build statistics collector
2. Implement cost model
3. Generate multiple query plans
4. Choose lowest-cost plan

---

## 4. Aggregate Functions

### GROUP BY Implementation

```
SELECT department, COUNT(*), AVG(salary)
FROM employees
GROUP BY department

Algorithm:
1. Scan table
2. Group by department (HashMap<String, List<Tuple>>)
3. For each group:
   - COUNT: size()
   - AVG: sum / size()
4. Return aggregated results
```

---

# Summary

## PostgresLite Architecture Highlights

### High-Level Design
- **Layered architecture**: Presentation → Parser → Command → Catalog/Model → Storage
- **File-based persistence**: Simple but functional
- **Interactive CLI**: User-friendly interface with colored output
- **Modular design**: Clear separation of concerns

### Low-Level Design
- **Command Pattern**: Encapsulates SQL operations
- **Singleton Pattern**: Single catalog instance
- **Strategy Pattern**: Flexible parsing
- **Tuple-level locking**: Concurrency control

### Key Features
- **SQL Support**: CREATE, INSERT, SELECT (with JOINs), UPDATE, DELETE, ALTER
- **Advanced queries**: WHERE, ORDER BY, LIMIT, OFFSET, JOIN operations
- **Persistence**: File-based with Java serialization
- **Concurrency**: Read locks on tuples

### Production Readiness
- ✓ Educational implementation demonstrating database internals
- ✓ Comprehensive test coverage
- ✓ Clean, maintainable code
- ✓ Extensible architecture

### Learning Value
This project demonstrates:
- Database architecture and design
- SQL parsing and execution
- Storage engine fundamentals
- Query processing algorithms
- Concurrency control basics
- Design patterns in practice

**Note**: PostgresLite is designed for educational purposes to understand database internals, not for production use.
