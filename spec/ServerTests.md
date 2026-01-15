# Server Test Specification

This document defines the comprehensive test suite for the StoreFront server. It serves as the single source of truth for test coverage, ensuring all functional and security requirements are verified.

## 1. Authentication & Security
**Goal**: Verify the integrity of the authentication system and role-based access control (RBAC).

### 1.1 Authentication Flows
| Scenario | Pre-condition | Steps | Expected Result |
| :--- | :--- | :--- | :--- |
| **Successful Login** | User exists | POST `/auth/login` with valid creds | `200 OK` with JWT token |
| **Failed Login** | User exists | POST `/auth/login` with invalid password | `401 Unauthorized` |
| **Token Expiry** | Expired Token | GET Protected Resource | `401 Unauthorized` |
| **Registration** | Admin Token | POST `/auth/register` with new user | `200 OK`, user created |

### 1.2 Role-Based Access Control (RBAC)
| Scenario | Pre-condition | Steps | Expected Result |
| :--- | :--- | :--- | :--- |
| **Admin Access** | Admin Token | GET `/auth/users` | `200 OK`, list of users |
| **Employee Access Denied** | Employee Token | GET `/auth/users` | `403 Forbidden` |
| **Store Access Boundary** | User assigned to Store A | Access Store B Resource | `403 Forbidden` |

### 1.3 Security Hardening
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **SQL Injection** | Login with `' OR '1'='1` | `401 Unauthorized` (Input sanitized/Parametrized query) |
| **XSS Prevention** | Register user with `<script>...` name | Sanitized on output or Rejected |

## 2. Inventory Management
**Goal**: Ensure product and stock accuracy across the system.

### 2.1 Product & Bundle Lifecycle
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Create Product** | Admin creates "Book A" | `200 OK`, ID returned |
| **Create Bundle** | Admin creates Bundle "Set 1" (Book A + Pen B) | `200 OK`, Bundle Validated |
| **Soft Delete** | Delete Product "Book A" | Product `is_active=false`, not removed from DB |

### 2.2 Stock Management
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Add Stock (Master)** | Add 100 units to Master Store | Master Stock = Previous + 100 |
| **Allocation** | Allocate 50 units from Master to Virtual Store | Master -50, Virtual +50 |
| **Over-Allocation** | Allocate 150 units (only 100 avail) | `400 Bad Request`, Stock unchanged |

## 3. Order Processing
**Goal**: Verify order placement, validation, and stock deduction.

### 3.1 Order Creation
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Standard Order** | Buy 2 items from Virtual Store | `200 OK`, Stock reduced in Virtual Store |
| **Bundle Order** | Buy 1 Bundle | Individual items reduced from Stock |
| **Insufficient Stock** | Buy more than available | `400 Bad Request` |
| **Invalid Store** | Order from non-existent store | `404 Not Found` |

### 3.2 Reconciliation
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Stock Reconciliation** | Return unsold items from Virtual to Master | Virtual Stock = 0, Master Stock restored |
| **Report Generation** | Trigger Reconciliation | JSON Report generated with sales figures |

## 4. Multi-Store Architecture
**Goal**: Validate isolation and management of multiple virtual stores.

### 4.1 Store Management
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Create Store** | Admin creates "Store X" | `200 OK`, Store created |
| **Assign User** | Assign User U to Store X | User can manage Store X |
| **Cross-Store Data**| User U (Store X) tries to view Store Y stock | `403 Forbidden` or Empty List |

## 5. System Resilience
**Goal**: Verify system stability under stress.

### 5.1 Concurrency
| Scenario | Steps | Expected Result |
| :--- | :--- | :--- |
| **Concurrent Orders** | 10 users buy last item simultaneously | Only 1 succeeds, 9 fail with `409` or `400` |
| **Concurrent Alloc** | High volume stock updates | Final stock count matches net transactions |
