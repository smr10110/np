# Package: repository

This package contains the **repository interfaces** (Spring Data JPA) that provide database access for the Commerce module entities.

### Responsibilities

1.  **Data Access:** Extend `JpaRepository` to offer standard CRUD operations.
2.  **Custom Queries:** Define complex retrieval methods using Query Methods (naming conventions) or `@Query` annotations.

### Main Classes

* **`CommerceRepository`:** Primary repository for the `Commerce` entity.
* **`CommerceCategoryRepository`:** Repository for the `CommerceCategory` entity.
* **`CommerceValidationRepository`:** Repository for the `CommerceValidation` entity.