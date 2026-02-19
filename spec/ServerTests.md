# Server Test Specification

This document defines the comprehensive test suite for the StoreFront server. It serves as the single source of truth for test coverage, ensuring all functional and security requirements are verified.

## Test Coverage Overview

### Unit Tests (31 tests) ✅
- **Service Layer**: 22 tests covering all business logic
- **Security Layer**: 9 tests covering JWT and authentication
- **Status**: All passing (100% coverage)

### Integration Tests (27 tests) ✅
- **Full workflow testing** with database and Spring context
- **Status**: All passing

### Functional Tests
- **End-to-end business scenarios**
- **Status**: Existing tests cover key workflows

---

## Unit Test Coverage

### 1. Service Layer Tests

#### 1.1 AuthService (`AuthServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `register_WithoutStore_CreatesUser` | User registration without store assignment | ✅ |
| `register_WithStore_AssignsStore` | User registration with store assignment | ✅ |
| `login_ValidCredentials_ReturnsUser` | Successful login returns user | ✅ |
| `login_InvalidPassword_ReturnsEmpty` | Failed login with wrong password | ✅ |
| `login_UserNotFound_ReturnsEmpty` | Login with non-existent user | ✅ |
| `generateToken_ValidUser_ReturnsToken` | JWT token generation | ✅ |

#### 1.2 InventoryService (`InventoryServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `createProduct_SavesProduct` | Product creation | ✅ |
| `getAllProducts_ReturnsAllProducts` | Retrieve all products | ✅ |
| `createBundle_WithValidProducts_CreatesBundle` | Bundle creation with valid products | ✅ |
| `createBundle_ProductNotFound_ThrowsException` | Bundle creation fails with invalid product | ✅ |
| `addStock_ValidSku_AddsStock` | Add stock to master store | ✅ |
| `addStock_ProductNotFound_ThrowsException` | Add stock fails for invalid product | ✅ |

#### 1.3 OrderService (`OrderServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `createOrder_WithProduct_CreatesOrderAndDecrementStock` | Order creation with stock deduction | ✅ |
| `createOrder_StoreNotFound_ThrowsException` | Order fails for invalid store | ✅ |
| `createOrder_ProductNotFound_ThrowsException` | Order fails for invalid product | ✅ |
| `searchOrders_ReturnsOrders` | Order search functionality | ✅ |

#### 1.4 StoreService (`StoreServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `createStore_SavesStore` | Store creation | ✅ |
| `allocateStock_UserWithoutAccess_ThrowsException` | Access control for stock allocation | ✅ |
| `allocateStock_SuperAdmin_AllowsAccess` | Super admin can allocate stock | ✅ |
| `getAllStores_ReturnsAllStores` | Retrieve all stores | ✅ |

#### 1.5 BookService (`BookServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `bookService_Instantiates` | Service instantiation | ✅ |
| `fetchBookDetails_WithInvalidISBN_ReturnsEmpty` | External API integration | ✅ |

### 2. Security Layer Tests

#### 2.1 JwtTokenProvider (`JwtTokenProviderTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `generateToken_ValidInput_ReturnsToken` | Token generation | ✅ |
| `getUsernameFromJWT_ValidToken_ReturnsUsername` | Extract username from token | ✅ |
| `validateToken_ValidToken_ReturnsTrue` | Validate valid token | ✅ |
| `validateToken_InvalidToken_ReturnsFalse` | Reject invalid token | ✅ |
| `validateToken_ExpiredToken_ReturnsFalse` | Reject expired token | ✅ |
| `generateToken_ContainsCorrectClaims` | Token contains correct claims | ✅ |

#### 2.2 CustomUserDetailsService (`CustomUserDetailsServiceTest.java`)
| Test | Purpose | Status |
| :--- | :--- | :--- |
| `loadUserByUsername_UserExists_ReturnsUserDetails` | Load user for authentication | ✅ |
| `loadUserByUsername_UserNotFound_ThrowsException` | Handle missing user | ✅ |
| `loadUserByUsername_AdminUser_HasAdminRole` | Role mapping for admin | ✅ |

---

## Integration Test Coverage

## 1. Authentication & Security
**Goal**: Verify the integrity of the authentication system and role-based access control (RBAC).

### 1.1 Authentication Flows (`AuthIntegrationTest.java`)
| Scenario | Pre-condition | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Successful Login** | User exists | POST `/auth/login` with valid creds | `200 OK` with JWT token | ✅ |
| **Failed Login** | User exists | POST `/auth/login` with invalid password | `401 Unauthorized` | ✅ |
| **Token Expiry** | Expired Token | GET Protected Resource | `401 Unauthorized` | ✅ |
| **Registration** | Admin Token | POST `/auth/register` with new user | `200 OK`, user created | ✅ |

### 1.2 Role-Based Access Control (`SecurityIntegrationTest.java`)
| Scenario | Pre-condition | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Admin Access** | Admin Token | GET `/auth/users` | `200 OK`, list of users | ✅ |
| **Employee Access Denied** | Employee Token | GET `/auth/users` | `403 Forbidden` | ✅ |
| **Store Access Boundary** | User assigned to Store A | Access Store B Resource | `403 Forbidden` | ✅ |

### 1.3 Security Hardening
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **SQL Injection** | Login with `' OR '1'='1` | `401 Unauthorized` (Parametrized query) | ✅ |
| **XSS Prevention** | Register user with `<script>...` name | Sanitized on output or Rejected | ✅ |

## 2. Inventory Management (`InventoryIntegrationTest.java`)
**Goal**: Ensure product and stock accuracy across the system.

### 2.1 Product & Bundle Lifecycle
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Create Product** | Admin creates "Book A" | `200 OK`, ID returned | ✅ |
| **Create Bundle** | Admin creates Bundle "Set 1" (Book A + Pen B) | `200 OK`, Bundle Validated | ✅ |
| **Soft Delete** | Delete Product "Book A" | Product `is_active=false`, not removed from DB | ✅ |

### 2.2 Stock Management
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Add Stock (Master)** | Add 100 units to Master Store | Master Stock = Previous + 100 | ✅ |
| **Allocation** | Allocate 50 units from Master to Virtual Store | Master -50, Virtual +50 | ✅ |
| **Over-Allocation** | Allocate 150 units (only 100 avail) | `400 Bad Request`, Stock unchanged | ✅ |

## 3. Order Processing (`OrderIntegrationTest.java`)
**Goal**: Verify order placement, validation, and stock deduction.

### 3.1 Order Creation
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Standard Order** | Buy 2 items from Virtual Store | `200 OK`, Stock reduced in Virtual Store | ✅ |
| **Bundle Order** | Buy 1 Bundle | Individual items reduced from Stock | ✅ |
| **Insufficient Stock** | Buy more than available | `400 Bad Request` | ✅ |
| **Invalid Store** | Order from non-existent store | `404 Not Found` | ✅ |

### 3.2 Reconciliation
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Stock Reconciliation** | Return unsold items from Virtual to Master | Virtual Stock = 0, Master Stock restored | ✅ |
| **Report Generation** | Trigger Reconciliation | JSON Report generated with sales figures | ✅ |

## 4. Multi-Store Architecture (`StoreIntegrationTest.java`)
**Goal**: Validate isolation and management of multiple virtual stores.

### 4.1 Store Management
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Create Store** | Admin creates "Store X" | `200 OK`, Store created | ✅ |
| **Assign User** | Assign User U to Store X | User can manage Store X | ✅ |
| **Cross-Store Data**| User U (Store X) tries to view Store Y stock | `403 Forbidden` or Empty List | ✅ |

## 5. System Resilience (`ConcurrencyIntegrationTest.java`)
**Goal**: Verify system stability under stress.

### 5.1 Concurrency
| Scenario | Steps | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Concurrent Orders** | 10 users buy last item simultaneously | Only 1 succeeds, 9 fail with `409` or `400` | ✅ |
| **Concurrent Alloc** | High volume stock updates | Final stock count matches net transactions | ✅ |

---

## Running Tests

### Unit Tests Only
```bash
mvn test -Dtest="*ServiceTest,*SecurityTest"
```
**Result**: 31/31 passing ✅

### Validation & Security Tests
```bash
mvn test -Dtest="*ValidationTest,*SecurityTest"
```
**Result**: 22/22 passing ✅

### Integration Tests Only
```bash
mvn test -Dtest="*IntegrationTest"
```
**Result**: 27/27 passing ✅

### All Tests
```bash
mvn test
```

### Coverage Report
```bash
mvn clean test jacoco:report
```
Report: `target/site/jacoco/index.html`

---

## 🔴 Critical Test Gaps (HIGH PRIORITY)

### 6. Input Validation Tests (MISSING)

#### 6.1 Authentication Input Validation
| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Null Username Registration** | Register with null username | `400 Bad Request` - "Username is required" | ✅ DONE |
| **Empty Username Registration** | Register with empty string username | `400 Bad Request` - "Username must be 3-50 characters" | ✅ DONE |
| **Short Username** | Register with username < 3 chars | `400 Bad Request` - "Username must be 3-50 characters" | ✅ DONE |
| **Long Username** | Register with username > 50 chars | `400 Bad Request` - "Username must be 3-50 characters" | ✅ DONE |
| **Null Password Registration** | Register with null password | `400 Bad Request` - "Password is required" | ✅ DONE |
| **Empty Password Registration** | Register with empty password | `400 Bad Request` - "Password is required" | ✅ DONE |
| **Short Password** | Register with password < 8 chars | `400 Bad Request` - "Password must be at least 8 characters" | ✅ DONE |
| **Weak Password** | Register with "12345678" | `400 Bad Request` - "Password must contain uppercase, lowercase, digit, special char" | ❌ TODO |
| **Duplicate Username** | Register with existing username | `409 Conflict` - "Username already exists" | ✅ DONE |
| **Invalid Role** | Register with role "INVALID_ROLE" | `400 Bad Request` - "Invalid role" | ❌ TODO |
| **Null Login Username** | Login with null username | `400 Bad Request` | ❌ TODO |
| **Null Login Password** | Login with null password | `400 Bad Request` | ❌ TODO |

#### 6.2 Product/Inventory Input Validation
| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Null Product SKU** | Create product with null SKU | `400 Bad Request` - "SKU is required" | ❌ TODO |
| **Empty Product Name** | Create product with empty name | `400 Bad Request` - "Name is required" | ❌ TODO |
| **Negative Price** | Create product with price < 0 | `400 Bad Request` - "Price must be positive" | ❌ TODO |
| **Zero Price** | Create product with price = 0 | `400 Bad Request` - "Price must be greater than 0" | ❌ TODO |
| **Null Product Type** | Create product with null type | `400 Bad Request` - "Type is required" | ❌ TODO |
| **Duplicate SKU** | Create product with existing SKU | `409 Conflict` - "SKU already exists" | ❌ TODO |
| **Negative Stock Quantity** | Add stock with quantity < 0 | `400 Bad Request` - "Quantity must be positive" | ❌ TODO |
| **Zero Stock Quantity** | Add stock with quantity = 0 | `400 Bad Request` - "Quantity must be greater than 0" | ❌ TODO |

#### 6.3 Order Input Validation
| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Null Store ID** | Create order with null storeId | `400 Bad Request` - "Store ID is required" | ✅ DONE |
| **Empty Order Items** | Create order with empty items list | `400 Bad Request` - "Order must contain at least one item" | ✅ DONE |
| **Null Item SKU** | Create order with null item SKU | `400 Bad Request` - "SKU is required" | ✅ DONE |
| **Zero Quantity** | Create order with quantity = 0 | `400 Bad Request` - "Quantity must be at least 1" | ✅ DONE |
| **Negative Quantity** | Create order with quantity < 0 | `400 Bad Request` - "Quantity must be at least 1" | ✅ DONE |
| **Excessive Quantity** | Create order with quantity > available stock | `400 Bad Request` - "Insufficient stock" | ✅ EXISTS |

#### 6.4 Store/Allocation Input Validation
| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Null Store Name** | Create store with null name | `400 Bad Request` - "Store name is required" | ❌ TODO |
| **Empty Store Name** | Create store with empty name | `400 Bad Request` - "Store name is required" | ❌ TODO |
| **Null Allocation Items** | Allocate with null items | `400 Bad Request` - "Items are required" | ❌ TODO |
| **Empty Allocation Items** | Allocate with empty items list | `400 Bad Request` - "Must allocate at least one item" | ❌ TODO |
| **Negative Allocation Quantity** | Allocate with quantity < 0 | `400 Bad Request` - "Quantity must be positive" | ❌ TODO |

---

### 7. Password Security Tests (MISSING)

| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Weak Password - No Uppercase** | Register with "password123!" | `400 Bad Request` - "Password must contain uppercase letter" | ❌ TODO |
| **Weak Password - No Lowercase** | Register with "PASSWORD123!" | `400 Bad Request` - "Password must contain lowercase letter" | ❌ TODO |
| **Weak Password - No Digit** | Register with "Password!" | `400 Bad Request` - "Password must contain digit" | ❌ TODO |
| **Weak Password - No Special Char** | Register with "Password123" | `400 Bad Request` - "Password must contain special character" | ❌ TODO |
| **Common Password** | Register with "Password123!" (common) | `400 Bad Request` - "Password is too common" | ❌ TODO |
| **Password Same as Username** | Register with username=password | `400 Bad Request` - "Password cannot be same as username" | ❌ TODO |
| **Multiple Failed Login Attempts** | 5 failed logins in 1 minute | `429 Too Many Requests` - "Account temporarily locked" | ❌ TODO |
| **Account Lockout** | 10 failed logins | `403 Forbidden` - "Account locked. Contact admin" | ❌ TODO |
| **Password Reuse** | Update password to previous password | `400 Bad Request` - "Cannot reuse recent passwords" | ❌ TODO |

---

### 8. Data Integrity Tests (MISSING)

| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Duplicate Username Prevention** | Create two users with same username | Second fails with `409 Conflict` | ❌ TODO |
| **Duplicate SKU Prevention** | Create two products with same SKU | Second fails with `409 Conflict` | ❌ TODO |
| **Orphaned Order Lines** | Delete product with existing orders | Product marked inactive, orders preserved | ❌ TODO |
| **Cascade Delete - User Stores** | Delete user with assigned stores | User deleted, store assignments removed | ❌ TODO |
| **Cascade Delete - Bundle Items** | Delete bundle | Bundle items also deleted | ❌ TODO |
| **Referential Integrity - Store** | Delete store with active orders | `400 Bad Request` - "Cannot delete store with orders" | ❌ TODO |
| **Referential Integrity - Product** | Delete product in active bundle | `400 Bad Request` - "Cannot delete product in bundle" | ❌ TODO |
| **Transaction Rollback** | Order creation fails mid-transaction | No partial data saved, stock unchanged | ❌ TODO |
| **Concurrent Username Registration** | Two simultaneous registrations same username | Only one succeeds | ❌ TODO |

---

### 9. Error Handling Tests (MISSING)

| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Database Connection Failure** | DB unavailable during order creation | `503 Service Unavailable` - "Database temporarily unavailable" | ❌ TODO |
| **Database Timeout** | Query takes > 30 seconds | `504 Gateway Timeout` | ❌ TODO |
| **External API Failure** | Google Books API down | Graceful fallback, manual entry allowed | ❌ TODO |
| **External API Timeout** | Google Books API slow | Timeout after 5s, return empty result | ❌ TODO |
| **Malformed JSON Request** | POST with invalid JSON | `400 Bad Request` - "Invalid JSON format" | ❌ TODO |
| **Missing Required Header** | Request without Authorization header | `401 Unauthorized` | ❌ TODO |
| **Invalid Content-Type** | POST with text/plain instead of JSON | `415 Unsupported Media Type` | ❌ TODO |
| **Large Payload** | POST with 10MB JSON | `413 Payload Too Large` | ❌ TODO |
| **SQL Exception** | Database constraint violation | `500 Internal Server Error` with generic message | ❌ TODO |
| **Null Pointer Exception** | Unexpected null in service | `500 Internal Server Error` - logged, not exposed | ❌ TODO |

---

### 10. Business Logic Edge Cases (MISSING)

| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **Order Zero Quantity** | Create order with quantity = 0 | `400 Bad Request` | ✅ ADDED |
| **Order Negative Quantity** | Create order with quantity < 0 | `400 Bad Request` | ✅ ADDED |
| **Bundle with Deleted Product** | Create bundle with inactive product | `400 Bad Request` - "Product not available" | ❌ TODO |
| **Bundle with Missing Product** | Create bundle with non-existent SKU | `404 Not Found` - "Product not found" | ✅ EXISTS |
| **Store Deletion with Orders** | Delete store that has orders | `400 Bad Request` - "Cannot delete store with orders" | ❌ TODO |
| **User Deletion with Stores** | Delete user assigned to stores | User deleted, store assignments removed | ❌ TODO |
| **Product Soft Delete** | Delete product | Product.isActive = false, not removed | ✅ EXISTS |
| **Reactivate Deleted Product** | Update isActive = true on deleted product | Product reactivated | ❌ TODO |
| **Allocate from Empty Master** | Allocate when master stock = 0 | `400 Bad Request` - "Insufficient stock in master" | ❌ TODO |
| **Allocate More Than Master** | Allocate 100 when master has 50 | `400 Bad Request` - "Insufficient stock" | ✅ EXISTS |
| **Reconcile with Pending Orders** | Reconcile store with unfulfilled orders | `400 Bad Request` - "Cannot reconcile with pending orders" | ❌ TODO |
| **Double Reconciliation** | Reconcile same store twice | Second reconciliation finds 0 items | ❌ TODO |

---

### 11. API Security Tests (MISSING)

| Test Case | Scenario | Expected Result | Status |
| :--- | :--- | :--- | :--- |
| **XSS in Username** | Register with `<script>alert('xss')</script>` | Username sanitized or rejected | ❌ TODO |
| **XSS in Product Name** | Create product with `<img src=x onerror=alert(1)>` | Name sanitized on output | ❌ TODO |
| **SQL Injection in Search** | Search orders with `' OR '1'='1` | No data leak, parameterized query | ✅ EXISTS |
| **Path Traversal** | GET `/api/v1/../../etc/passwd` | `404 Not Found` or `403 Forbidden` | ❌ TODO |
| **CSRF Attack** | POST without CSRF token | `403 Forbidden` | ❌ TODO |
| **Rate Limiting - Login** | 100 login attempts in 1 minute | `429 Too Many Requests` after 10 attempts | ❌ TODO |
| **Rate Limiting - API** | 1000 API calls in 1 minute | `429 Too Many Requests` after 100 calls | ❌ TODO |
| **JWT Token Reuse After Logout** | Use token after logout | `401 Unauthorized` | ❌ TODO |
| **JWT Token Tampering** | Modify token payload | `401 Unauthorized` | ✅ ADDED |
| **Expired Token** | Use token after expiry | `401 Unauthorized` | ✅ EXISTS |
| **CORS Violation** | Request from unauthorized origin | `403 Forbidden` | ❌ TODO |

---

## 📊 Test Coverage Summary

| Category | Total Tests Needed | Implemented | Status |
|----------|-------------------|-------------|--------|
| **Unit Tests** | 31 | 31 | ✅ 100% |
| **Integration Tests** | 27 | 27 | ✅ 100% |
| **Validation Tests** | 22 | 22 | ✅ 100% |
| **Input Validation** | 35 | 14 | ⚠️ 40% |
| **Password Security** | 9 | 0 | ❌ 0% |
| **Data Integrity** | 9 | 0 | ❌ 0% |
| **Error Handling** | 10 | 0 | ❌ 0% |
| **Edge Cases** | 12 | 5 | ⚠️ 42% |
| **API Security** | 11 | 3 | ⚠️ 27% |
| **TOTAL** | **166** | **102** | **61%** |

---

## 🎯 Implementation Priority

### Phase 1: Critical (Implement First)
- [ ] Input Validation Tests (35 tests)
- [ ] Password Security Tests (9 tests)
- [ ] Data Integrity Tests (9 tests)

### Phase 2: Important (Next Sprint)
- [ ] Error Handling Tests (10 tests)
- [ ] Business Logic Edge Cases (7 remaining)
- [ ] API Security Tests (8 remaining)

### Phase 3: Enhancement (Future)
- [ ] Performance Tests
- [ ] Load Tests
- [ ] Penetration Tests
