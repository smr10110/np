# Domain Package

This package defines the core business entities for the reporting module.

## Main Classes

- **Transaction**: Represents a payment transaction, mapped to the `payment_transaction` table in the database. It includes fields for origin and destination accounts, date, amount, status, commerce, and description. This entity is used throughout the reporting services for filtering, grouping, and exporting transaction data.
