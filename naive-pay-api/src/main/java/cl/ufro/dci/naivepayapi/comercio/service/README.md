# Package: service

This package contains the **Service layer** (business logic) for the Commerce module.

### Responsibilities

1.  **Core Logic:** House the rules, validations, and workflow for all module operations.
2.  **Transactions:** Ensure data atomicity and integrity by orchestrating calls to repositories within transactions.
3.  **Logic Separation:** Divide responsibilities between primary CRUD management and specialized processes (such as validation).

### Main Classes

* **`CommerceManagementService`:** Implements the primary CRUD logic (Create, Read, Update, Delete) for the `Commerce` entity.
* **`CommerceValidationService`:** Contains the specific logic for initiating, processing, and finalizing the commerce validation and verification process.