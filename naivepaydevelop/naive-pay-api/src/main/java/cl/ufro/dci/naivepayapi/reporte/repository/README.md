# Repository Package

This package provides interfaces for database access using Spring Data JPA.

## Main Interfaces

- **TransactionRepository**: Extends `JpaRepository` for CRUD operations on `Transaction` entities. It defines a custom query method `findFilteredTransactions` that supports dynamic filtering by user, date, status, commerce, description, and amount. This method is used by services to retrieve transactions matching user-specified criteria.
