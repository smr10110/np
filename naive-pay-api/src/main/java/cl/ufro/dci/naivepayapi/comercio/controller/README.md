## Controller Package
This package have the controller classes for the endpoints

## Main Classes

### **CommerceCategoryController**

REST Controller, exposes an endpoint in `/categories` to list all the categories in the database. 

| Endpoint      | Type | Description                                   | Request Body | Response                             | Code |
|---------------|------|-----------------------------------------------|--------------|--------------------------------------|------|
| `/categories` | GET  | Return the list of categories in the database | -            | `<List<CommerceCategory> categories` | 200  |


### **CommerceController**

REST Controller that handles operations on commerces.

| Endpoint            | Type   | Description                               | Request Body           | Response                            | Code |
|---------------------|--------|-------------------------------------------|------------------------|-------------------------------------|------|
| `/commerce`         | GET    | Return a string with api status           | -                      | `String message`                    | 200  |
| `/commerce`         | POST   | Create a new commerce                     | `CommerceCreation dto` | `CommerceResponse commerceResponse` | 201  |
| `/commerce/all`     | GET    | Return a list with all commerces          | -                      | `List<CommerceResponse> dtos`       | 200  |
| `/commerce/{taxId}` | GET    | Return a commerce with the same `taxId`   | -                      | `CommerceResponse commerceResponse` | 200  |
| `/commerce/{taxId}` | PUT    | Update the commerce with the same `taxId` | `CommerceUpdate dto`   | `CommerceResponse updatedCommerce`  | 200  |
| `/commerce/{taxId}` | DELETE | Delete the commerce with the same `taxId` | -                      | *empty*                             | 204  |


### **CommerceValidationController**

REST controller to manage commerce validation.

| Endpoint                   | Type | Description                                                                               | Request Body                    | Response                                           | Code |
|----------------------------|------|-------------------------------------------------------------------------------------------|---------------------------------|----------------------------------------------------|------|
| `/validation`              | GET  | Return a String with the api status                                                       | -                               | `String message`                                   | 200  |
| `/validation/submit`       | POST | Create a new commerce                                                                     | `CommerceValidationRequest dto` | `CommerceValidation newRequest`                    | 201  |
| `/validation/pending`      | GET  | Return a list with all pending commerces                                                  | -                               | `List<CommerceValidation> pendingRequests`         | 200  |
| `/validation/expired`      | GET  | Return a list with all expired commerces                                                  | -                               | `List<CommerceValidation> expiredRequestsRequests` | 200  |
| `/validation/approve/{id}` | PUT  | Return a commerce with the same `taxId`                                                   | `CommerceApproval dto`          | `Commerce approvedCommeerce`                       | 200  |
| `/validation/rejecti/{id}` | PUT  | Change the validation status (PENDING to REJECTED) for the commerce with the same `taxId` | -                               | *empty*                                            | 204  |
