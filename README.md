# PostgresLite

A lightweight, educational implementation of a PostgreSQL-like relational database management system built in Java. PostgresLite provides a command-line interface for executing SQL-like queries with file-based persistence.

## Features

### Core Functionality
- **Interactive CLI**: Beautiful command-line interface with colored output
- **File-based Storage**: Persistent data storage using custom serialization
- **SQL Command Support**: Familiar SQL-like syntax for database operations
- **Catalog Management**: Metadata management for tables and columns
- **Comprehensive Testing**: Full test coverage for all major features

### Supported SQL Commands

#### CREATE TABLE
Create new tables with column definitions:
```sql
CREATE TABLE users (id INT, name STRING, age INT)
```

#### INSERT INTO
Insert data into tables:
```sql
INSERT INTO users VALUES (1, 'Alice', 30)
INSERT INTO users VALUES (2, 'Bob', 25)
```

#### SELECT
Query data with advanced filtering and sorting:
```sql
-- Basic select
SELECT * FROM users

-- With WHERE clause
SELECT * FROM users WHERE id = 1
SELECT * FROM users WHERE name = 'Alice'

-- IS NULL / IS NOT NULL
SELECT * FROM users WHERE age IS NULL
SELECT * FROM users WHERE age IS NOT NULL

-- ORDER BY (ascending or descending)
SELECT * FROM users ORDER BY age ASC
SELECT * FROM users ORDER BY age DESC

-- LIMIT and OFFSET for pagination
SELECT * FROM users LIMIT 10
SELECT * FROM users LIMIT 10 OFFSET 5
SELECT * FROM users ORDER BY id ASC LIMIT 10 OFFSET 5
```

#### JOIN Operations
Supports INNER, LEFT, and RIGHT joins:
```sql
-- INNER JOIN
SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id

-- LEFT JOIN
SELECT * FROM users LEFT JOIN orders ON users.id = orders.user_id

-- RIGHT JOIN
SELECT * FROM users RIGHT JOIN orders ON users.id = orders.user_id

-- JOIN with WHERE clause (qualified columns)
SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id WHERE users.id = 1
SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id WHERE orders.amount = 100

-- JOIN with ORDER BY
SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id ORDER BY users.name ASC
SELECT * FROM users INNER JOIN orders ON users.id = orders.user_id ORDER BY orders.id DESC
```

#### UPDATE
Modify existing records:
```sql
UPDATE users SET name = 'Bob Smith' WHERE id = 2
UPDATE users SET age = 31 WHERE name = 'Alice'
```

#### DELETE
Remove records from tables:
```sql
DELETE FROM users WHERE id = 1
DELETE FROM users WHERE name = 'Bob'
```

#### ALTER TABLE
Add new columns to existing tables:
```sql
ALTER TABLE users ADD COLUMN email STRING
ALTER TABLE users ADD COLUMN score FLOAT
```

### Supported Data Types
- `INT` - Integer numbers
- `STRING` - Text/character data
- `BOOLEAN` - True/false values
- `FLOAT` - Decimal numbers

## Architecture

The project follows a clean, modular architecture:

```
src/main/java/com/postgresql/
├── cli/                    # Command-line interface
│   ├── PostgresLiteShell.java    # Main entry point
│   └── ConsoleUI.java             # UI formatting and colors
├── command/                # Command pattern implementations
│   ├── Command.java              # Base command interface
│   ├── CreateTableCommand.java
│   ├── InsertCommand.java
│   ├── SelectCommand.java
│   ├── UpdateCommand.java
│   ├── DeleteCommand.java
│   └── AlterTableCommand.java
├── parser/                 # SQL parsing
│   └── CommandParser.java        # Parse SQL strings to commands
├── catalog/                # Metadata management
│   ├── CatalogManager.java       # Singleton catalog manager
│   ├── TableMetadata.java        # Table schema info
│   └── ColumnMetadata.java       # Column definitions
├── model/                  # Data models
│   ├── Table.java                # Table abstraction
│   └── Tuple.java                # Row/record representation
├── storage/                # Persistence layer
│   ├── TableHeap.java            # In-memory table data
│   └── TableSerializer.java      # File I/O operations
├── common/                 # Shared utilities
│   ├── DataType.java             # Supported data types enum
│   └── CommandConstants.java     # SQL keyword constants
└── exception/              # Custom exceptions
    ├── TableNotFoundException.java
    └── InvalidSyntaxException.java
```

### Key Design Patterns
- **Command Pattern**: Each SQL operation is encapsulated as a command object
- **Singleton Pattern**: CatalogManager ensures single source of metadata truth
- **Strategy Pattern**: Different parsers for different SQL command types
- **Repository Pattern**: TableSerializer handles all persistence logic

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation & Setup

1. Clone the repository:
```bash
cd postgresq-lite-master
```

2. Build the project:
```bash
mvn clean install
```

3. Run the interactive shell:
```bash
mvn exec:java
```

Or alternatively, run the compiled class directly:
```bash
java -cp target/postgresql-lite-1.0-SNAPSHOT.jar com.postgresql.cli.PostgresLiteShell
```

### Running Tests

Execute all tests:
```bash
mvn test
```

Run specific test suites:
```bash
# Basic operations
mvn test -Dtest=DatabaseBasicTest

# JOIN operations
mvn test -Dtest=DatabaseJoinTest

# Advanced SELECT features
mvn test -Dtest=DatabaseSelectAdvancedTest

# Concurrency tests
mvn test -Dtest=DatabaseConcurrencyTest
```

## Usage Examples

### Interactive Shell

When you start the shell, you'll see a colorful welcome message:

```
╔══════════════════════════════════════════════╗
║                                              ║
║   🛠️  Welcome to the PostgresLite CLI         ║
║                                              ║
║   Type 'exit' to quit, and may the queries   ║
║   be ever in your favor. 🔍⚡               ║
║                                              ║
╚══════════════════════════════════════════════╝

postgres-lite>
```

### Example Session

```sql
postgres-lite> CREATE TABLE employees (id INT, name STRING, salary FLOAT, active BOOLEAN)
✅ Table 'employees' created successfully

postgres-lite> INSERT INTO employees VALUES (1, 'John Doe', 75000.50, true)
✅ Inserted 1 row

postgres-lite> INSERT INTO employees VALUES (2, 'Jane Smith', 82000.00, true)
✅ Inserted 1 row

postgres-lite> INSERT INTO employees VALUES (3, 'Bob Johnson', 65000.00, false)
✅ Inserted 1 row

postgres-lite> SELECT * FROM employees
[1, John Doe, 75000.5, true]
[2, Jane Smith, 82000.0, true]
[3, Bob Johnson, 65000.0, false]

postgres-lite> SELECT * FROM employees WHERE active = true ORDER BY salary DESC
[2, Jane Smith, 82000.0, true]
[1, John Doe, 75000.5, true]

postgres-lite> UPDATE employees SET salary = 80000.00 WHERE id = 1
✅ Updated 1 row(s)

postgres-lite> ALTER TABLE employees ADD COLUMN department STRING
✅ Column 'department' added to table 'employees'

postgres-lite> DELETE FROM employees WHERE id = 3
✅ Deleted 1 row(s)

postgres-lite> exit
👋 Exiting PostgresLite. Goodbye!
```

### Programmatic Usage

You can also use PostgresLite programmatically in your Java applications:

```java
import com.postgresql.catalog.CatalogManager;
import com.postgresql.catalog.ColumnMetadata;
import com.postgresql.common.DataType;
import com.postgresql.model.Table;
import com.postgresql.model.Tuple;

import java.util.Arrays;

public class Example {
    public static void main(String[] args) {
        // Get catalog instance
        CatalogManager catalog = CatalogManager.getInstance();

        // Create a table
        catalog.createTable("products", Arrays.asList(
            new ColumnMetadata("id", DataType.INT),
            new ColumnMetadata("name", DataType.STRING),
            new ColumnMetadata("price", DataType.FLOAT)
        ));

        // Get table reference
        Table products = catalog.getTable("products");

        // Insert data
        products.insertTuple(new Tuple(Arrays.asList(1, "Laptop", 999.99)));
        products.insertTuple(new Tuple(Arrays.asList(2, "Mouse", 29.99)));

        // Query data
        products.getAllTuples().forEach(System.out::println);
    }
}
```

## Data Persistence

PostgresLite uses file-based storage in the `data/` directory. For each table, two files are created:

- `<tablename>.table` - Stores table metadata (schema information)
- `<tablename>.tbl` - Stores actual table data (serialized tuples)

Example:
```
data/
├── users.table
├── users.tbl
├── orders.table
└── orders.tbl
```

## Testing

The project includes comprehensive test coverage:

### Test Suites

1. **DatabaseBasicTest**: Tests fundamental CRUD operations
   - Table creation
   - Insert and select operations
   - Basic data integrity

2. **DatabaseSelectAdvancedTest**: Tests advanced query features
   - ORDER BY (ASC/DESC)
   - LIMIT and OFFSET
   - Pagination combinations

3. **DatabaseJoinTest**: Tests JOIN operations
   - INNER JOIN
   - LEFT JOIN with NULL handling
   - RIGHT JOIN with NULL handling
   - Qualified column names in WHERE/ORDER BY
   - IS NULL / IS NOT NULL filters
   - Edge cases with no matches

4. **DatabaseConcurrencyTest**: Tests thread safety
   - Concurrent read/write operations
   - Data consistency under load

## Future Enhancements

See [ToDo.md](ToDo.md) for planned features:

1. **PRIMARY KEY and UNIQUE Constraints**
   - Column-level uniqueness enforcement
   - Primary key validation

2. **Indexing Support**
   - Hash and B-tree index structures
   - Query optimization using indexes

3. **Transaction Support**
   - BEGIN, COMMIT, ROLLBACK commands
   - ACID compliance

4. **Concurrency Control**
   - Multi-Version Concurrency Control (MVCC)
   - Row-level locking

## Technical Details

### Technology Stack
- **Language**: Java 17
- **Build Tool**: Maven 3.x
- **Testing**: JUnit 5
- **Architecture**: Command Pattern, Singleton Pattern

### Project Configuration
- **Group ID**: com.postgresql
- **Artifact ID**: postgresql-lite
- **Version**: 1.0-SNAPSHOT
- **Main Class**: com.postgresql.cli.PostgresLiteShell

## Contributing

This is a personal educational project demonstrating database internals and design patterns. Feel free to fork and experiment!

## License

This project is available for educational purposes.

## Acknowledgments

Built as an educational implementation to understand:
- Relational database internals
- SQL parsing and execution
- Storage engine design
- Query optimization basics
- File-based persistence mechanisms

---

**Note**: PostgresLite is designed for learning purposes and is not suitable for production use. For production workloads, use established database systems like PostgreSQL, MySQL, or similar.
