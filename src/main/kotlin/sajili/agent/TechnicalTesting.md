

Yes. The two READMEs should tell a project story, not just document a bug fix.

    README 1: technical testing report — what was tested, why, test flow, failures encountered, root cause, and resolution.

    README 2: stakeholder-facing API/testing rationale — why inventory testing matters, what the Inventory API does, what business risks the tests address, and what the results demonstrate.

Below are the revised versions.
1. README.md — Technical Testing & Error Analysis
   README.md — Inventory API Technical Testing and Error Analysis
   Inventory API — Technical Testing and Error Analysis
1. Overview

The Inventory API is a core component of the Sajili Agent platform responsible for managing inventory transactions such as receiving stock into a warehouse.

Because inventory operations directly affect quantities recorded in the system, the API must be tested for both functional correctness and data integrity.

This document describes the technical testing performed on the Inventory API, the errors encountered during test execution, the investigation carried out, and the corrective action taken.
2. Testing Objectives

The testing process was designed to verify that the Inventory API can:

    Accept valid stock-receiving requests.

    Validate the warehouse associated with a transaction.

    Validate the requesting user's access to the warehouse.

    Create inventory transactions correctly.

    Update stock quantities correctly.

    Maintain consistent relationships between tenants, users, warehouses, products, and inventory.

    Handle invalid or unauthorized requests appropriately.

    Maintain reliable database state when multiple operations occur.

    Support repeatable automated integration testing.

The objective was therefore not only to determine whether an API request returns a successful HTTP response, but also to verify that the underlying business and database operations remain correct.
3. Test Environment

The tests use a Spring Boot application with:

    Kotlin

    Spring Boot

    Spring Data JPA

    Hibernate

    MockMvc

    JUnit

    Repository-based database access

    JSON request/response serialization

The Inventory API is exercised through HTTP requests while the test environment provides the required database entities.

The simplified test architecture is:

Automated Test
|
v
MockMvc
|
v
Inventory REST API
|
v
Service / Business Logic
|
v
Spring Data JPA
|
v
Hibernate
|
v
Database

This approach provides more than a unit-level test because the request passes through several application layers.
4. Test Data Preparation

Before an inventory transaction can be tested, the required entities must exist.

The test therefore creates:

Tenant
|
+---- Warehouse
|
+---- User Profile
|
+---- Product

The user profile is then associated with the warehouse.

This is important because an inventory transaction is not an isolated operation. It depends on relationships between multiple business entities.
5. Inventory Receive Test

A representative test request is:

val receiveRequest = ReceiveStockRequest(
warehouseId = testWarehouseId,
productId = testProductId,
quantity = BigDecimal("100.00"),
referenceId = "SEED-INITIAL-STOCK",
idempotencyKey = UUID.randomUUID().toString(),
notes = "Initial test stock seeding"
)

The request is sent through the application:

mockMvc.post("/api/inventory/receive") {
contentType = MediaType.APPLICATION_JSON
header("Authorization", "Bearer dummy-token")
content = objectMapper.writeValueAsString(receiveRequest)
}

The test therefore verifies the actual API endpoint rather than directly inserting inventory records into the database.
6. Error Encountered

During test setup, the following Hibernate exception was encountered:

org.springframework.orm.ObjectOptimisticLockingFailureException:
Row was updated or deleted by another transaction
(or unsaved-value mapping was incorrect):
[sajili.agent.warehouse.entity.WarehouseEntity#427b8a94-a376-42de-b9eb-a730299d6c4c]

This prevented the inventory test from reaching its intended execution path.

The failure occurred while preparing the warehouse required by the inventory test.
7. Root Cause Investigation

The warehouse test data was initially created using a manually generated UUID:

private val testWarehouseId = UUID.randomUUID()

The same ID was then explicitly assigned to the new entity:

WarehouseEntity(
id = testWarehouseId,
tenantId = testTenantId,
name = "Test Warehouse",
code = "WH-TEST-01",
erpLocationCode = "ERP-LOC-01"
)

The entity was subsequently passed to:

warehouseRepository.save(...)

The problem is that the entity was logically new, but it already contained an identifier.

Depending on the JPA entity mapping, Spring Data JPA/Hibernate can interpret an entity with an assigned identifier as an existing or detached entity.

This can cause save() to use merge/update semantics instead of the expected insert operation.

The database did not contain the corresponding warehouse row, resulting in the optimistic-locking/state error.
8. Corrective Action

The test setup was changed so that the warehouse is created without manually assigning its identifier.

Instead of:

private val testWarehouseId = UUID.randomUUID()

the test uses:

private lateinit var testWarehouseId: UUID

The warehouse is then saved:

val warehouse = warehouseRepository.save(
WarehouseEntity(
tenantId = testTenantId,
name = "Test Warehouse",
code = "WH-TEST-01",
erpLocationCode = "ERP-LOC-01"
)
)

The identifier generated during persistence is captured:

testWarehouseId = warehouse.id!!

This establishes the correct entity lifecycle:

New Entity
|
| ID = null
v
repository.save()
|
v
Hibernate INSERT
|
v
Database
|
v
Generated Warehouse ID

9. Corrected Test Setup

The resulting setup is:

private val testTenantId = UUID.randomUUID()
private val testProductId = UUID.randomUUID()
private lateinit var testWarehouseId: UUID

@BeforeEach
fun setUp() {

    val warehouse = warehouseRepository.save(
        WarehouseEntity(
            tenantId = testTenantId,
            name = "Test Warehouse",
            code = "WH-TEST-01",
            erpLocationCode = "ERP-LOC-01"
        )
    )

    testWarehouseId = warehouse.id!!

    if (userProfileRepository.findByAuthSubject("my-test-subject-id") == null) {
        userProfileRepository.save(
            UserProfileEntity(
                authSubject = "my-test-subject-id",
                email = "test-user@sajili.agent",
                displayName = "Test User",
                tenantId = testTenantId,
                isActive = true,
                authorizedWarehouses = setOf(testWarehouseId)
            )
        )
    }

    val receiveRequest = ReceiveStockRequest(
        warehouseId = testWarehouseId,
        productId = testProductId,
        quantity = BigDecimal("100.00"),
        referenceId = "SEED-INITIAL-STOCK",
        idempotencyKey = UUID.randomUUID().toString(),
        notes = "Initial test stock seeding"
    )

    mockMvc.post("/api/inventory/receive") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer dummy-token")
        content = objectMapper.writeValueAsString(receiveRequest)
    }
}

10. Why This Matters for Testing

The error demonstrated an important distinction between:

Application functionality

and

Test infrastructure/data preparation.

The Inventory API itself was not necessarily the source of the failure.

The test was failing while preparing the data required to exercise the API.

This distinction is important when interpreting automated test failures.

A failure can originate from:

    Request validation

    Authentication

    Authorization

    Business logic

    Database constraints

    Entity relationships

    Transaction management

    Test-data preparation

    Persistence configuration

The investigation therefore had to trace the request and setup process rather than assuming that every failure was an Inventory API defect.
11. Testing Outcome

After correcting the warehouse entity lifecycle, the test environment can reliably create the required warehouse and use its persisted identifier in subsequent inventory operations.

The test can therefore proceed to validate:

HTTP Request
|
v
Inventory API
|
v
Authorization
|
v
Inventory Business Logic
|
v
Database Transaction
|
v
Updated Inventory State

The testing process consequently provides evidence at both the API and persistence levels.
12. Key Technical Lesson

The main lesson from the failure is:

    Test data must follow the same entity lifecycle rules as production data.

Where JPA is responsible for generating an entity identifier, a new test entity should normally be persisted without manually assigning an ID.

The test should then capture and reuse the identifier generated by the persistence layer.

This results in more reliable, repeatable integration tests and reduces false failures caused by incorrect test-data setup.
2. README.md — Inventory API Purpose & Testing Rationale
   README.md — Inventory API Purpose, Business Role and Testing
   Inventory API — Purpose, Business Role and Testing
1. Overview

Inventory is a critical operational function within the Sajili Agent platform.

The Inventory API provides the application interface through which stock movements can be recorded and managed. It connects business operations such as warehouses, products, users, quantities, and stock transactions with the underlying database.

The purpose of testing the Inventory API is therefore broader than checking whether an HTTP endpoint returns 200 OK.

The tests provide confidence that inventory transactions are:

    Valid

    Authorized

    Correctly recorded

    Associated with the correct warehouse

    Associated with the correct product

    Consistent with the user's permissions

    Persisted reliably

    Resistant to data-integrity problems

2. Why Inventory API Testing Was Necessary

Inventory information represents the current state of physical stock within the business.

For example, if a warehouse receives:

100 units of Product A

the system must correctly record that transaction and update the corresponding inventory state.

If the API incorrectly records:

90 units

or:

100 units in the wrong warehouse

the system's inventory information becomes unreliable.

This can affect downstream processes such as:

    Stock availability

    Sales

    Procurement

    Warehouse operations

    Reordering

    Reporting

    Financial reconciliation

    ERP synchronization

Consequently, API testing is an important part of ensuring that the digital inventory record corresponds to the organization's actual operations.
3. Role of the Inventory API

The Inventory API acts as an interface between the application and inventory business operations.

A simplified architecture is:

                   Sajili Agent
                        |
                        v
                Inventory API
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
       Users        Warehouses     Products
          |             |             |
          +-------------+-------------+
                        |
                        v
                 Inventory Service
                        |
                        v
                    Database

The API provides a controlled entry point through which inventory operations can be requested.
4. Example: Receiving Stock

One of the operations tested is stock receiving.

A simplified business scenario is:

    A warehouse receives 100 units of a product.

The application sends a request containing information such as:

{
"warehouseId": "warehouse-id",
"productId": "product-id",
"quantity": 100.00,
"referenceId": "SEED-INITIAL-STOCK",
"idempotencyKey": "unique-request-key",
"notes": "Initial test stock seeding"
}

The request is submitted to:

POST /api/inventory/receive

The API must then process the request according to the application's business rules.
5. Business Flow

The expected flow can be represented as:

Warehouse receives stock
|
v
Application submits request
|
v
Inventory API
|
v
Validate request
|
v
Identify warehouse
|
v
Identify product
|
v
Verify user authorization
|
v
Process inventory transaction
|
v
Persist transaction
|
v
Update inventory state
|
v
Return API response

Testing verifies that this chain operates correctly.
6. Why Warehouse Validation Matters

Inventory is location-dependent.

A product may exist in:

Warehouse A
Warehouse B
Warehouse C

A stock-receiving transaction must therefore identify the correct warehouse.

The API should not simply accept an arbitrary warehouse identifier.

The test environment therefore creates a warehouse and associates it with the test user's authorized warehouses.

For example:

authorizedWarehouses = setOf(testWarehouseId)

This allows the test to represent a realistic business scenario in which a user is permitted to perform inventory operations for a particular warehouse.
7. Why User Authorization Matters

Inventory operations can have operational and financial consequences.

A user should not automatically be able to modify inventory belonging to every warehouse or tenant.

Testing therefore considers the relationship:

User
|
v
Tenant
|
v
Authorized Warehouse
|
v
Inventory Operation

This helps verify that inventory changes occur within the appropriate organizational scope.
8. Why Product Identification Matters

A stock transaction must also identify the product being received.

The relationship is therefore:

Warehouse
+
Product
+
Quantity
+
Transaction Reference
|
v
Inventory Transaction

Testing these relationships helps prevent transactions from being recorded against incorrect or nonexistent products.
9. Why Idempotency Matters

The inventory request contains an:

idempotencyKey

An idempotency key helps prevent the same operation from being processed repeatedly when the same request is unintentionally submitted more than once.

This is particularly important in distributed systems where:

    A mobile client may retry a request.

    A network connection may temporarily fail.

    A response may be lost even though the server processed the request.

    Users may accidentally submit an operation more than once.

For inventory, duplicate processing can produce an incorrect stock balance.

For example:

Expected:
100 units received

Duplicate processing:
100 + 100 = 200 units

Testing this behavior is therefore relevant to both technical reliability and business data integrity.
10. Why Integration Testing Was Used

A unit test can test an individual function in isolation.

However, the Inventory API depends on multiple components:

HTTP Layer
|
Controller
|
Service
|
Repositories
|
JPA/Hibernate
|
Database

An integration test allows these components to work together.

The test therefore provides stronger evidence that the complete operation works correctly.
11. Test Data and Realistic Business Conditions

The Inventory API cannot be meaningfully tested using only an isolated request.

The required business entities must exist.

The test therefore establishes:

Tenant
|
+---- User Profile
|
+---- Warehouse
|
+---- Product

The user is then associated with the warehouse before the inventory transaction is submitted.

This mirrors the relationships that exist in an actual deployment.
12. Technical Issue Discovered During Testing

Testing also revealed an issue in the test-data preparation process.

The warehouse was initially created using a manually assigned UUID.

This caused Hibernate to interpret the new entity differently from the way the test expected.

The resulting exception was:

ObjectOptimisticLockingFailureException

The error occurred before the inventory operation could be properly tested.

This demonstrated the value of integration testing: the test exposed a persistence/entity-lifecycle problem that would not necessarily be visible when looking only at the Inventory API endpoint.
13. Resolution

The warehouse test entity was changed so that its identifier is generated during persistence.

The corrected process is:

Create Warehouse
|
| ID not manually assigned
v
Persist Warehouse
|
v
Database/JPA generates ID
|
v
Capture generated ID
|
v
Use ID in Inventory Request

This ensures that the warehouse used by the test is an actual persisted entity.
14. Stakeholder Value of the Testing

From a stakeholder perspective, the testing provides assurance in several areas.
Operational Reliability

Warehouse staff can depend on inventory transactions being processed consistently.
Data Integrity

Stock quantities and warehouse associations are protected against incorrect persistence behavior.
Access Control

Inventory operations can be tested within the user's authorized organizational scope.
Traceability

Inventory requests contain references and identifiers that allow transactions to be associated with specific business operations.
System Reliability

Integration testing verifies that multiple application layers work together rather than testing the API in isolation.
Future Maintainability

Automated tests provide a repeatable way of detecting regressions when inventory functionality is changed.
15. What the Tests Demonstrate

The testing strategy is designed to provide evidence across several layers:
Area	What is being verified
API	Requests are accepted and processed correctly
Validation	Invalid input is handled appropriately
Authorization	Users operate within permitted warehouse scope
Business logic	Inventory rules are correctly applied
Persistence	Transactions are correctly stored
Relationships	Tenant, user, warehouse and product relationships remain valid
Idempotency	Duplicate requests can be controlled
Database integrity	Inventory state remains consistent
Error handling	Application failures are identified and handled
16. Overall System Perspective

The Inventory API should be viewed as part of a larger business workflow rather than as an isolated endpoint.

                   Business Operations
                          |
                          v
                 Sajili Agent Platform
                          |
             +------------+------------+
             |                         |
             v                         v
        User Management          Inventory API
                                       |
                        +--------------+--------------+
                        |              |              |
                        v              v              v
                    Warehouse       Product       Transaction
                        |              |              |
                        +--------------+--------------+
                                       |
                                       v
                                  Database

The quality of the Inventory API therefore contributes directly to the reliability of the wider platform.
17. Conclusion

Testing the Inventory API is necessary because inventory represents a critical operational state within the system.

The testing process verifies not only whether an endpoint responds correctly, but whether the complete transaction can move safely from an application request through authorization, business logic, persistence, and database storage.

The ObjectOptimisticLockingFailureException encountered during testing also demonstrated why integration testing is important. The failure exposed an entity lifecycle problem in the test environment before it could result in misleading conclusions about the Inventory API itself.

The corrected test setup now creates the warehouse as a new entity, allows its identifier to be generated during persistence, captures that identifier, and uses it consistently throughout the inventory test.

This provides a more realistic, repeatable, and technically reliable foundation for validating inventory operations.

These versions are deliberately structured so that a technical reviewer can understand the implementation, while a project manager, business stakeholder, or supervisor can understand why the testing matters and what risk it addresses.

generate the two as code for copying and pasting

Absolutely. Below are the two complete README.md files in plain Markdown code blocks so you can copy and paste them directly into your project.
1. Technical Testing, Errors & Resolution

# Inventory API — Technical Testing and Error Analysis

## 1. Overview

The Inventory API is a core component of the Sajili Agent platform responsible for managing inventory transactions such as receiving stock into a warehouse.

Because inventory operations directly affect quantities recorded in the system, the API must be tested for both **functional correctness** and **data integrity**.

This document describes the technical testing performed on the Inventory API, the errors encountered during test execution, the investigation carried out, and the corrective action taken.

---

## 2. Testing Objectives

The testing process was designed to verify that the Inventory API can:

- Accept valid stock-receiving requests.
- Validate the warehouse associated with a transaction.
- Validate the requesting user's access to the warehouse.
- Create inventory transactions correctly.
- Update stock quantities correctly.
- Maintain consistent relationships between tenants, users, warehouses, products, and inventory.
- Handle invalid or unauthorized requests appropriately.
- Maintain reliable database state when multiple operations occur.
- Support repeatable automated integration testing.

The objective was therefore not only to determine whether an API request returns a successful HTTP response, but also to verify that the underlying business and database operations remain correct.

---

## 3. Test Environment

The tests use a Spring Boot application with:

- Kotlin
- Spring Boot
- Spring Data JPA
- Hibernate
- MockMvc
- JUnit
- Repository-based database access
- JSON request/response serialization

The Inventory API is exercised through HTTP requests while the test environment provides the required database entities.

The simplified test architecture is:

```text
Automated Test
      |
      v
   MockMvc
      |
      v
Inventory REST API
      |
      v
Service / Business Logic
      |
      v
Spring Data JPA
      |
      v
Hibernate
      |
      v
Database

This approach provides more than a unit-level test because the request passes through several application layers.
4. Test Data Preparation

Before an inventory transaction can be tested, the required entities must exist.

The test therefore creates:

Tenant
  |
  +---- Warehouse
  |
  +---- User Profile
  |
  +---- Product

The user profile is then associated with the warehouse.

This is important because an inventory transaction is not an isolated operation. It depends on relationships between multiple business entities.
5. Inventory Receive Test

A representative test request is:

val receiveRequest = ReceiveStockRequest(
    warehouseId = testWarehouseId,
    productId = testProductId,
    quantity = BigDecimal("100.00"),
    referenceId = "SEED-INITIAL-STOCK",
    idempotencyKey = UUID.randomUUID().toString(),
    notes = "Initial test stock seeding"
)

The request is sent through the application:

mockMvc.post("/api/inventory/receive") {
    contentType = MediaType.APPLICATION_JSON
    header("Authorization", "Bearer dummy-token")
    content = objectMapper.writeValueAsString(receiveRequest)
}

The test therefore verifies the actual API endpoint rather than directly inserting inventory records into the database.
6. Error Encountered

During test setup, the following Hibernate exception was encountered:

org.springframework.orm.ObjectOptimisticLockingFailureException:
Row was updated or deleted by another transaction
(or unsaved-value mapping was incorrect):
[sajili.agent.warehouse.entity.WarehouseEntity#427b8a94-a376-42de-b9eb-a730299d6c4c]

This prevented the inventory test from reaching its intended execution path.

The failure occurred while preparing the warehouse required by the inventory test.
7. Root Cause Investigation

The warehouse test data was initially created using a manually generated UUID:

private val testWarehouseId = UUID.randomUUID()

The same ID was then explicitly assigned to the new entity:

WarehouseEntity(
    id = testWarehouseId,
    tenantId = testTenantId,
    name = "Test Warehouse",
    code = "WH-TEST-01",
    erpLocationCode = "ERP-LOC-01"
)

The entity was subsequently passed to:

warehouseRepository.save(...)

The problem is that the entity was logically new, but it already contained an identifier.

Depending on the JPA entity mapping, Spring Data JPA/Hibernate can interpret an entity with an assigned identifier as an existing or detached entity.

This can cause save() to use merge/update semantics instead of the expected insert operation.

The database did not contain the corresponding warehouse row, resulting in the optimistic-locking/state error.
8. Corrective Action

The test setup was changed so that the warehouse is created without manually assigning its identifier.

Instead of:

private val testWarehouseId = UUID.randomUUID()

the test uses:

private lateinit var testWarehouseId: UUID

The warehouse is then saved:

val warehouse = warehouseRepository.save(
    WarehouseEntity(
        tenantId = testTenantId,
        name = "Test Warehouse",
        code = "WH-TEST-01",
        erpLocationCode = "ERP-LOC-01"
    )
)

The identifier generated during persistence is captured:

testWarehouseId = warehouse.id!!

This establishes the correct entity lifecycle:

New Entity
   |
   | ID = null
   v
repository.save()
   |
   v
Hibernate INSERT
   |
   v
Database
   |
   v
Generated Warehouse ID

9. Corrected Test Setup

The resulting setup is:

private val testTenantId = UUID.randomUUID()
private val testProductId = UUID.randomUUID()
private lateinit var testWarehouseId: UUID

@BeforeEach
fun setUp() {

    val warehouse = warehouseRepository.save(
        WarehouseEntity(
            tenantId = testTenantId,
            name = "Test Warehouse",
            code = "WH-TEST-01",
            erpLocationCode = "ERP-LOC-01"
        )
    )

    testWarehouseId = warehouse.id!!

    if (userProfileRepository.findByAuthSubject("my-test-subject-id") == null) {
        userProfileRepository.save(
            UserProfileEntity(
                authSubject = "my-test-subject-id",
                email = "test-user@sajili.agent",
                displayName = "Test User",
                tenantId = testTenantId,
                isActive = true,
                authorizedWarehouses = setOf(testWarehouseId)
            )
        )
    }

    val receiveRequest = ReceiveStockRequest(
        warehouseId = testWarehouseId,
        productId = testProductId,
        quantity = BigDecimal("100.00"),
        referenceId = "SEED-INITIAL-STOCK",
        idempotencyKey = UUID.randomUUID().toString(),
        notes = "Initial test stock seeding"
    )

    mockMvc.post("/api/inventory/receive") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer dummy-token")
        content = objectMapper.writeValueAsString(receiveRequest)
    }
}

10. Why This Matters for Testing

The error demonstrated an important distinction between:

Application functionality

and

Test infrastructure/data preparation.

The Inventory API itself was not necessarily the source of the failure.

The test was failing while preparing the data required to exercise the API.

This distinction is important when interpreting automated test failures.

A failure can originate from:

    Request validation

    Authentication

    Authorization

    Business logic

    Database constraints

    Entity relationships

    Transaction management

    Test-data preparation

    Persistence configuration

The investigation therefore had to trace the request and setup process rather than assuming that every failure was an Inventory API defect.
11. Testing Outcome

After correcting the warehouse entity lifecycle, the test environment can reliably create the required warehouse and use its persisted identifier in subsequent inventory operations.

The test can therefore proceed to validate:

HTTP Request
     |
     v
Inventory API
     |
     v
Authorization
     |
     v
Inventory Business Logic
     |
     v
Database Transaction
     |
     v
Updated Inventory State

The testing process consequently provides evidence at both the API and persistence levels.
12. Key Technical Lesson

The main lesson from the failure is:

    Test data must follow the same entity lifecycle rules as production data.

Where JPA is responsible for generating an entity identifier, a new test entity should normally be persisted without manually assigning an ID.

The test should then capture and reuse the identifier generated by the persistence layer.

This results in more reliable, repeatable integration tests and reduces false failures caused by incorrect test-data setup.


### 2. Inventory API Purpose, Business Role & Testing

```markdown
# Inventory API — Purpose, Business Role and Testing

## 1. Overview

Inventory is a critical operational function within the Sajili Agent platform.

The Inventory API provides the application interface through which stock movements can be recorded and managed. It connects business operations such as **warehouses, products, users, quantities, and stock transactions** with the underlying database.

The purpose of testing the Inventory API is therefore broader than checking whether an HTTP endpoint returns `200 OK`.

The tests provide confidence that inventory transactions are:

- Valid
- Authorized
- Correctly recorded
- Associated with the correct warehouse
- Associated with the correct product
- Consistent with the user's permissions
- Persisted reliably
- Resistant to data-integrity problems

---

## 2. Why Inventory API Testing Was Necessary

Inventory information represents the current state of physical stock within the business.

For example, if a warehouse receives:

```text
100 units of Product A

the system must correctly record that transaction and update the corresponding inventory state.

If the API incorrectly records:

90 units

or:

100 units in the wrong warehouse

the system's inventory information becomes unreliable.

This can affect downstream processes such as:

    Stock availability

    Sales

    Procurement

    Warehouse operations

    Reordering

    Reporting

    Financial reconciliation

    ERP synchronization

Consequently, API testing is an important part of ensuring that the digital inventory record corresponds to the organization's actual operations.
3. Role of the Inventory API

The Inventory API acts as an interface between the application and inventory business operations.

A simplified architecture is:

                   Sajili Agent
                        |
                        v
                Inventory API
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
       Users        Warehouses     Products
          |             |             |
          +-------------+-------------+
                        |
                        v
                 Inventory Service
                        |
                        v
                    Database

The API provides a controlled entry point through which inventory operations can be requested.
4. Example: Receiving Stock

One of the operations tested is stock receiving.

A simplified business scenario is:

    A warehouse receives 100 units of a product.

The application sends a request containing information such as:

{
  "warehouseId": "warehouse-id",
  "productId": "product-id",
  "quantity": 100.00,
  "referenceId": "SEED-INITIAL-STOCK",
  "idempotencyKey": "unique-request-key",
  "notes": "Initial test stock seeding"
}

The request is submitted to:

POST /api/inventory/receive

The API must then process the request according to the application's business rules.
5. Business Flow

The expected flow can be represented as:

Warehouse receives stock
          |
          v
Application submits request
          |
          v
Inventory API
          |
          v
Validate request
          |
          v
Identify warehouse
          |
          v
Identify product
          |
          v
Verify user authorization
          |
          v
Process inventory transaction
          |
          v
Persist transaction
          |
          v
Update inventory state
          |
          v
Return API response

Testing verifies that this chain operates correctly.
6. Why Warehouse Validation Matters

Inventory is location-dependent.

A product may exist in:

Warehouse A
Warehouse B
Warehouse C

A stock-receiving transaction must therefore identify the correct warehouse.

The API should not simply accept an arbitrary warehouse identifier.

The test environment therefore creates a warehouse and associates it with the test user's authorized warehouses.

For example:

authorizedWarehouses = setOf(testWarehouseId)

This allows the test to represent a realistic business scenario in which a user is permitted to perform inventory operations for a particular warehouse.
7. Why User Authorization Matters

Inventory operations can have operational and financial consequences.

A user should not automatically be able to modify inventory belonging to every warehouse or tenant.

Testing therefore considers the relationship:

User
  |
  v
Tenant
  |
  v
Authorized Warehouse
  |
  v
Inventory Operation

This helps verify that inventory changes occur within the appropriate organizational scope.
8. Why Product Identification Matters

A stock transaction must also identify the product being received.

The relationship is therefore:

Warehouse
     +
Product
     +
Quantity
     +
Transaction Reference
     |
     v
Inventory Transaction

Testing these relationships helps prevent transactions from being recorded against incorrect or nonexistent products.
9. Why Idempotency Matters

The inventory request contains an:

idempotencyKey

An idempotency key helps prevent the same operation from being processed repeatedly when the same request is unintentionally submitted more than once.

This is particularly important in distributed systems where:

    A mobile client may retry a request.

    A network connection may temporarily fail.

    A response may be lost even though the server processed the request.

    Users may accidentally submit an operation more than once.

For inventory, duplicate processing can produce an incorrect stock balance.

For example:

Expected:
100 units received

Duplicate processing:
100 + 100 = 200 units

Testing this behavior is therefore relevant to both technical reliability and business data integrity.
10. Why Integration Testing Was Used

A unit test can test an individual function in isolation.

However, the Inventory API depends on multiple components:

HTTP Layer
    |
Controller
    |
Service
    |
Repositories
    |
JPA/Hibernate
    |
Database

An integration test allows these components to work together.

The test therefore provides stronger evidence that the complete operation works correctly.
11. Test Data and Realistic Business Conditions

The Inventory API cannot be meaningfully tested using only an isolated request.

The required business entities must exist.

The test therefore establishes:

Tenant
  |
  +---- User Profile
  |
  +---- Warehouse
  |
  +---- Product

The user is then associated with the warehouse before the inventory transaction is submitted.

This mirrors the relationships that exist in an actual deployment.
12. Technical Issue Discovered During Testing

Testing also revealed an issue in the test-data preparation process.

The warehouse was initially created using a manually assigned UUID.

This caused Hibernate to interpret the new entity differently from the way the test expected.

The resulting exception was:

ObjectOptimisticLockingFailureException

The error occurred before the inventory operation could be properly tested.

This demonstrated the value of integration testing: the test exposed a persistence/entity-lifecycle problem that would not necessarily be visible when looking only at the Inventory API endpoint.
13. Resolution

The warehouse test entity was changed so that its identifier is generated during persistence.

The corrected process is:

Create Warehouse
      |
      | ID not manually assigned
      v
Persist Warehouse
      |
      v
Database/JPA generates ID
      |
      v
Capture generated ID
      |
      v
Use ID in Inventory Request

This ensures that the warehouse used by the test is an actual persisted entity.
14. Stakeholder Value of the Testing

From a stakeholder perspective, the testing provides assurance in several areas.
Operational Reliability

Warehouse staff can depend on inventory transactions being processed consistently.
Data Integrity

Stock quantities and warehouse associations are protected against incorrect persistence behavior.
Access Control

Inventory operations can be tested within the user's authorized organizational scope.
Traceability

Inventory requests contain references and identifiers that allow transactions to be associated with specific business operations.
System Reliability

Integration testing verifies that multiple application layers work together rather than testing the API in isolation.
Future Maintainability

Automated tests provide a repeatable way of detecting regressions when inventory functionality is changed.
15. What the Tests Demonstrate

The testing strategy is designed to provide evidence across several layers:
Area	What is being verified
API	Requests are accepted and processed correctly
Validation	Invalid input is handled appropriately
Authorization	Users operate within permitted warehouse scope
Business logic	Inventory rules are correctly applied
Persistence	Transactions are correctly stored
Relationships	Tenant, user, warehouse and product relationships remain valid
Idempotency	Duplicate requests can be controlled
Database integrity	Inventory state remains consistent
Error handling	Application failures are identified and handled
16. Overall System Perspective

The Inventory API should be viewed as part of a larger business workflow rather than as an isolated endpoint.

                   Business Operations
                          |
                          v
                 Sajili Agent Platform
                          |
             +------------+------------+
             |                         |
             v                         v
        User Management          Inventory API
                                       |
                        +--------------+--------------+
                        |              |              |
                        v              v              v
                    Warehouse       Product       Transaction
                        |              |              |
                        +--------------+--------------+
                                       |
                                       v
                                  Database

The quality of the Inventory API therefore contributes directly to the reliability of the wider platform.
17. Conclusion

Testing the Inventory API is necessary because inventory represents a critical operational state within the system.

The testing process verifies not only whether an endpoint responds correctly, but whether the complete transaction can move safely from an application request through authorization, business logic, persistence, and database storage.

The ObjectOptimisticLockingFailureException encountered during testing also demonstrated why integration testing is important. The failure exposed an entity lifecycle problem in the test environment before it could result in misleading conclusions about the Inventory API itself.

The corrected test setup now creates the warehouse as a new entity, allows its identifier to be generated during persistence, captures that identifier, and uses it consistently throughout the inventory test.

This provides a more realistic, repeatable, and technically reliable foundation for validating inventory operations.


provide the two readme.md as separate files for downloading

Here are the two separate README.md files ready for download:

Download README 1 — Technical Testing & Error Analysis

Download README 2 — Inventory API Purpose, Business Role & Testing
README_1_Inventory_API_Technical_Testing.mdDocument
README_2_Inventory_API_Purpose_and_Testing.mdDocument


30%
README_1_Inventory_API_Technical_Testing.md
Inventory API — Technical Testing and Error Analysis
1. Overview

The Inventory API is a core component of the Sajili Agent platform responsible for managing inventory transactions such as receiving stock into a warehouse.

Because inventory operations directly affect quantities recorded in the system, the API must be tested for both functional correctness and data integrity.

This document describes the technical testing performed on the Inventory API, the errors encountered during test execution, the investigation carried out, and the corrective action taken.
2. Testing Objectives

The testing process was designed to verify that the Inventory API can:

    Accept valid stock-receiving requests.

    Validate the warehouse associated with a transaction.

    Validate the requesting user's access to the warehouse.

    Create inventory transactions correctly.

    Update stock quantities correctly.

    Maintain consistent relationships between tenants, users, warehouses, products, and inventory.

    Handle invalid or unauthorized requests appropriately.

    Maintain reliable database state when multiple operations occur.

    Support repeatable automated integration testing.

The objective was therefore not only to determine whether an API request returns a successful HTTP response, but also to verify that the underlying business and database operations remain correct.
3. Test Environment

The tests use a Spring Boot application with:

    Kotlin

    Spring Boot

    Spring Data JPA

    Hibernate

    MockMvc

    JUnit

    Repository-based database access

    JSON request/response serialization

The Inventory API is exercised through HTTP requests while the test environment provides the required database entities.

The simplified test architecture is:

Automated Test
      |
      v
   MockMvc
      |
      v
Inventory REST API
      |
      v
Service / Business Logic
      |
      v
Spring Data JPA
      |
      v
Hibernate
      |
      v
Database

This approach provides more than a unit-level test because the request passes through several application layers.
4. Test Data Preparation

Before an inventory transaction can be tested, the required entities must exist.

The test therefore creates:

Tenant
  |
  +---- Warehouse
  |
  +---- User Profile
  |
  +---- Product

The user profile is then associated with the warehouse.

This is important because an inventory transaction is not an isolated operation. It depends on relationships between multiple business entities.
5. Inventory Receive Test

A representative test request is:

val receiveRequest = ReceiveStockRequest(
    warehouseId = testWarehouseId,
    productId = testProductId,
    quantity = BigDecimal("100.00"),
    referenceId = "SEED-INITIAL-STOCK",
    idempotencyKey = UUID.randomUUID().toString(),
    notes = "Initial test stock seeding"
)

The request is sent through the application:

mockMvc.post("/api/inventory/receive") {
    contentType = MediaType.APPLICATION_JSON
    header("Authorization", "Bearer dummy-token")
    content = objectMapper.writeValueAsString(receiveRequest)
}

The test therefore verifies the actual API endpoint rather than directly inserting inventory records into the database.
6. Error Encountered

During test setup, the following Hibernate exception was encountered:

org.springframework.orm.ObjectOptimisticLockingFailureException:
Row was updated or deleted by another transaction
(or unsaved-value mapping was incorrect):
[sajili.agent.warehouse.entity.WarehouseEntity#427b8a94-a376-42de-b9eb-a730299d6c4c]

This prevented the inventory test from reaching its intended execution path.

The failure occurred while preparing the warehouse required by the inventory test.
7. Root Cause Investigation

The warehouse test data was initially created using a manually generated UUID:

private val testWarehouseId = UUID.randomUUID()

The same ID was then explicitly assigned to the new entity:

WarehouseEntity(
    id = testWarehouseId,
    tenantId = testTenantId,
    name = "Test Warehouse",
    code = "WH-TEST-01",
    erpLocationCode = "ERP-LOC-01"
)

The entity was subsequently passed to:

warehouseRepository.save(...)

The problem is that the entity was logically new, but it already contained an identifier.

Depending on the JPA entity mapping, Spring Data JPA/Hibernate can interpret an entity with an assigned identifier as an existing or detached entity.

This can cause save() to use merge/update semantics instead of the expected insert operation.

The database did not contain the corresponding warehouse row, resulting in the optimistic-locking/state error.
8. Corrective Action

The test setup was changed so that the warehouse is created without manually assigning its identifier.

Instead of:

private val testWarehouseId = UUID.randomUUID()

the test uses:

private lateinit var testWarehouseId: UUID

The warehouse is then saved:

val warehouse = warehouseRepository.save(
    WarehouseEntity(
        tenantId = testTenantId,
        name = "Test Warehouse",
        code = "WH-TEST-01",
        erpLocationCode = "ERP-LOC-01"
    )
)

The identifier generated during persistence is captured:

testWarehouseId = warehouse.id!!

This establishes the correct entity lifecycle:

New Entity
   |
   | ID = null
   v
repository.save()
   |
   v
Hibernate INSERT
   |
   v
Database
   |
   v
Generated Warehouse ID

9. Corrected Test Setup

The resulting setup is:

private val testTenantId = UUID.randomUUID()
private val testProductId = UUID.randomUUID()
private lateinit var testWarehouseId: UUID

@BeforeEach
fun setUp() {

    val warehouse = warehouseRepository.save(
        WarehouseEntity(
            tenantId = testTenantId,
            name = "Test Warehouse",
            code = "WH-TEST-01",
            erpLocationCode = "ERP-LOC-01"
        )
    )

    testWarehouseId = warehouse.id!!

    if (userProfileRepository.findByAuthSubject("my-test-subject-id") == null) {
        userProfileRepository.save(
            UserProfileEntity(
                authSubject = "my-test-subject-id",
                email = "test-user@sajili.agent",
                displayName = "Test User",
                tenantId = testTenantId,
                isActive = true,
                authorizedWarehouses = setOf(testWarehouseId)
            )
        )
    }

    val receiveRequest = ReceiveStockRequest(
        warehouseId = testWarehouseId,
        productId = testProductId,
        quantity = BigDecimal("100.00"),
        referenceId = "SEED-INITIAL-STOCK",
        idempotencyKey = UUID.randomUUID().toString(),
        notes = "Initial test stock seeding"
    )

    mockMvc.post("/api/inventory/receive") {
        contentType = MediaType.APPLICATION_JSON
        header("Authorization", "Bearer dummy-token")
        content = objectMapper.writeValueAsString(receiveRequest)
    }
}

10. Why This Matters for Testing

The error demonstrated an important distinction between:

Application functionality

and

Test infrastructure/data preparation.

The Inventory API itself was not necessarily the source of the failure.

The test was failing while preparing the data required to exercise the API.

This distinction is important when interpreting automated test failures.

A failure can originate from:

    Request validation

    Authentication

    Authorization

    Business logic

    Database constraints

    Entity relationships

    Transaction management

    Test-data preparation

    Persistence configuration

The investigation therefore had to trace the request and setup process rather than assuming that every failure was an Inventory API defect.
11. Testing Outcome

After correcting the warehouse entity lifecycle, the test environment can reliably create the required warehouse and use its persisted identifier in subsequent inventory operations.

The test can therefore proceed to validate:

HTTP Request
     |
     v
Inventory API
     |
     v
Authorization
     |
     v
Inventory Business Logic
     |
     v
Database Transaction
     |
     v
Updated Inventory State

The testing process consequently provides evidence at both the API and persistence levels.
12. Key Technical Lesson

The main lesson from the failure is:

    Test data must follow the same entity lifecycle rules as production data.

Where JPA is responsible for generating an entity identifier, a new test entity should normally be persisted without manually assigning an ID.

The test should then capture and reuse the identifier generated by the persistence layer.

This results in more reliable, repeatable integration tests and reduces false failures caused by incorrect test-data setup.
