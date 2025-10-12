# TODO: Advanced Features for PostgresLite

1. **Add support for PRIMARY KEY and UNIQUE constraints on columns**
   - Allow columns to be defined as PRIMARY KEY or UNIQUE during table creation.
   - Enforce uniqueness and non-null constraints for primary keys.
   - Enforce uniqueness for unique columns.

2. **Add Indexing Support**
   - Implement basic index structures (e.g., hash or B-tree) for faster query performance.
   - Integrate index usage in SELECT, UPDATE, and DELETE operations.

3. **Add Atomicity (Transactions)**
   - Support BEGIN, COMMIT, and ROLLBACK commands for transaction management.
   - Ensure all changes within a transaction are atomic and consistent.

4. **Implement Row Versioning / Concurrency Control**
   - Add Multi-Version Concurrency Control (MVCC) or basic row-level locking.
   - Allow safe concurrent access and modifications by multiple users/threads.
